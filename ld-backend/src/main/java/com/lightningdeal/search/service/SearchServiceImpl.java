package com.lightningdeal.search.service;

import com.lightningdeal.activity.entity.SeckillActivity;
import com.lightningdeal.search.entity.ActivityIndex;
import com.lightningdeal.search.model.EsSyncMessage;
import com.lightningdeal.search.repository.ActivitySearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.multiMatchQuery;

/**
 * ES 搜索服务实现
 *
 * 单个活动同步（syncActivity/deleteIndex）通过 MQ 异步处理，
 * 与 DB 操作解耦，失败自动重试。
 * 批量同步（syncActivities）直接调 ES，用于全量同步场景。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    value = "lightning-deal.search.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class SearchServiceImpl implements SearchService {

    private final ActivitySearchRepository repository;
    private final ElasticsearchRestTemplate elasticsearchTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Value("${lightning-deal.rabbitmq.es-sync-routing-key:seckill.es.sync}")
    private String esSyncRoutingKey;

    @Override
    public void syncActivity(SeckillActivity activity) {
        Runnable send = () -> {
            rabbitTemplate.convertAndSend("seckill.es.exchange", esSyncRoutingKey,
                    new EsSyncMessage("sync", activity.getId()));
            log.debug("MQ 发送 ES 同步消息 activityId={}", activity.getId());
        };
        // 如果当前有事务，等提交后再发 MQ
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    @Override
    public void syncActivities(List<SeckillActivity> activities) {
        List<ActivityIndex> indices = activities.stream().map(this::toIndex).collect(Collectors.toList());
        repository.saveAll(indices);
        log.info("ES 批量同步 {} 个活动", indices.size());
    }

    @Override
    public Page<ActivityIndex> search(String keyword, Pageable pageable) {
        // 使用 ik 分词多字段搜索 + 高亮
        org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder highlightBuilder =
                new org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder()
                .preTags("<em class='search-highlight'>")
                .postTags("</em>")
                .field("name")
                .field("goodsName")
                .field("goodsDescription");
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(multiMatchQuery(keyword, "name", "goodsName", "goodsDescription")
                        .analyzer("ik_max_word"))
                .withHighlightBuilder(highlightBuilder)
                .withPageable(pageable)
                .build();

        SearchHits<ActivityIndex> searchHits = elasticsearchTemplate.search(query, ActivityIndex.class);
        List<ActivityIndex> content = searchHits.stream()
                .map(hit -> {
                    ActivityIndex index = hit.getContent();
                    Map<String, List<String>> highlights = hit.getHighlightFields();
                    if (highlights.containsKey("name")) {
                        index.setName(highlights.get("name").get(0));
                    }
                    if (highlights.containsKey("goodsName")) {
                        index.setGoodsName(highlights.get("goodsName").get(0));
                    }
                    if (highlights.containsKey("goodsDescription")) {
                        index.setGoodsDescription(highlights.get("goodsDescription").get(0));
                    }
                    return index;
                })
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, searchHits.getTotalHits());
    }

    @Override
    public void deleteIndex(Long activityId) {
        Runnable send = () -> {
            rabbitTemplate.convertAndSend("seckill.es.exchange", esSyncRoutingKey,
                    new EsSyncMessage("delete", activityId));
            log.debug("MQ 发送 ES 删除消息 activityId={}", activityId);
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    private ActivityIndex toIndex(SeckillActivity activity) {
        ActivityIndex index = new ActivityIndex();
        BeanUtils.copyProperties(activity, index);
        return index;
    }
}
