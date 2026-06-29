package com.lightningdeal.search.service;

import com.lightningdeal.activity.entity.SeckillActivity;
import com.lightningdeal.search.entity.ActivityIndex;
import com.lightningdeal.search.repository.ActivitySearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.multiMatchQuery;

/**
 * ES 搜索服务实现
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

    @Override
    public void syncActivity(SeckillActivity activity) {
        ActivityIndex index = toIndex(activity);
        repository.save(index);
        log.debug("ES 同步活动 activityId={}, name={}", activity.getId(), activity.getName());
    }

    @Override
    public void syncActivities(List<SeckillActivity> activities) {
        List<ActivityIndex> indices = activities.stream().map(this::toIndex).collect(Collectors.toList());
        repository.saveAll(indices);
        log.info("ES 批量同步 {} 个活动", indices.size());
    }

    @Override
    public Page<ActivityIndex> search(String keyword, Pageable pageable) {
        // 使用 ik 分词多字段搜索
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(multiMatchQuery(keyword, "name", "goodsName", "goodsDescription")
                        .analyzer("ik_max_word"))
                .withPageable(pageable)
                .build();

        SearchHits<ActivityIndex> searchHits = elasticsearchTemplate.search(query, ActivityIndex.class);
        List<ActivityIndex> content = searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, searchHits.getTotalHits());
    }

    @Override
    public void deleteIndex(Long activityId) {
        repository.deleteById(activityId);
    }

    private ActivityIndex toIndex(SeckillActivity activity) {
        ActivityIndex index = new ActivityIndex();
        BeanUtils.copyProperties(activity, index);
        return index;
    }
}
