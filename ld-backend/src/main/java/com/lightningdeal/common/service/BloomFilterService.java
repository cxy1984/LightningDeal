package com.lightningdeal.common.service;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * 布隆过滤器服务 —— 防缓存穿透
 *
 * <p>使用 Redisson 的 RBloomFilter 实现，布隆过滤器存储在 Redis 中，
 * 应用重启不丢失，多实例共享。</p>
 *
 * <p>作用：在查询 DB 之前快速判断一个 activityId 是否可能存在，
 * 如果布隆过滤器判断"不存在"，则直接返回，避免恶意请求打穿到 MySQL。</p>
 *
 * <p>误判率：默认 1%（可通过构造参数调整），
 * 即最多有 1% 的"不存在"请求被误判为"可能存在"而穿透到 DB，
 * 但绝不会把"存在"误判为"不存在"。</p>
 */
@Slf4j
@Service
public class BloomFilterService {

    private static final String BLOOM_FILTER_NAME = "bloom:activity_ids";
    /** 预期元素数量 */
    private static final long EXPECTED_INSERTIONS = 10_000;
    /** 误判率 */
    private static final double FALSE_PROBABILITY = 0.01;

    private final RedissonClient redissonClient;
    private RBloomFilter<Long> bloomFilter;

    public BloomFilterService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 初始化布隆过滤器
     *
     * <p>如果 Redis 中已存在同名的布隆过滤器（应用重启），则直接使用。
     * 如果不存在则创建新的。</p>
     */
    @PostConstruct
    public void init() {
        bloomFilter = redissonClient.getBloomFilter(BLOOM_FILTER_NAME);
        if (!bloomFilter.isExists()) {
            boolean initialized = bloomFilter.tryInit(EXPECTED_INSERTIONS, FALSE_PROBABILITY);
            if (initialized) {
                log.info("布隆过滤器初始化完成，expectedInsertions={}, falseProbability={}",
                        EXPECTED_INSERTIONS, FALSE_PROBABILITY);
            } else {
                log.warn("布隆过滤器已存在，直接使用现有实例");
            }
        } else {
            log.info("布隆过滤器已存在，直接使用（expectedInsertions={}, falseProbability={}）",
                    bloomFilter.getExpectedInsertions(), bloomFilter.getFalseProbability());
        }
    }

    /**
     * 判断活动 ID 是否可能存在
     *
     * @param activityId 活动 ID
     * @return true = 可能存在（可能穿透到 DB）；false = 一定不存在（直接拦截）
     */
    public boolean mightContain(Long activityId) {
        if (activityId == null) {
            return false;
        }
        return bloomFilter.contains(activityId);
    }

    /**
     * 添加活动 ID 到布隆过滤器
     *
     * <p>在创建活动或预热库存时调用</p>
     */
    public void add(Long activityId) {
        if (activityId == null) {
            return;
        }
        bloomFilter.add(activityId);
        log.debug("布隆过滤器添加 activityId={}", activityId);
    }

    /**
     * 批量添加活动 ID
     *
     * <p>在应用启动时从 MySQL 加载所有有效活动 ID</p>
     */
    public void addAll(java.util.Collection<Long> activityIds) {
        if (activityIds == null || activityIds.isEmpty()) {
            return;
        }
        for (Long id : activityIds) {
            bloomFilter.add(id);
        }
        log.info("布隆过滤器批量添加 {} 个活动 ID", activityIds.size());
    }

    /**
     * 获取布隆过滤器当前已添加的元素数量
     */
    public long count() {
        return bloomFilter.count();
    }

    /**
     * 获取布隆过滤器的预期插入量（仅用于日志/监控）
     */
    public long getExpectedInsertions() {
        return bloomFilter.getExpectedInsertions();
    }

    /**
     * 获取布隆过滤器的误判率（仅用于日志/监控）
     */
    public double getFalseProbability() {
        return bloomFilter.getFalseProbability();
    }

    @PreDestroy
    public void destroy() {
        // RBloomFilter 由 Redisson 管理连接，无需手动关闭
        log.info("布隆过滤器服务销毁");
    }
}
