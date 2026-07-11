package com.lightningdeal.task;

import com.lightningdeal.activity.entity.SeckillActivity;
import com.lightningdeal.activity.service.SeckillActivityService;
import com.lightningdeal.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 活动状态自动流转定时任务
 *
 * 每分钟执行一次，处理：
 * 1. status=1（上架中）且 startTime < now → 设为 2（进行中），预热 Redis 库存
 * 2. status=2（进行中）且 endTime < now → 设为 3（已结束），清理 Redis 缓存
 *
 * 秒杀链路本身已通过 toVO() 实时计算状态，定时任务主要用于：
 * - 更新 DB status 使管理后台筛选准确
 * - 清理已结束活动的 Redis 缓存，释放内存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityStatusTask {

    private final SeckillActivityService activityService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SearchService searchService;

    private static final String STOCK_PREFIX = "seckill:stock:";
    private static final String SOLD_PREFIX = "seckill:sold:";
    private static final String USER_SET_PREFIX = "seckill:users:";
    private static final String USER_COUNT_PREFIX = "seckill:user_count:";
    private static final String RESULT_PREFIX = "seckill:result:";
    private static final String LOCK_PREFIX = "seckill:lock:";

    @Scheduled(fixedRate = 60000) // 每分钟执行
    public void syncActivityStatus() {
        LocalDateTime now = LocalDateTime.now();

        // === 1. 即将开始 → 进行中 ===
        List<SeckillActivity> toStart = activityService.lambdaQuery()
                .eq(SeckillActivity::getStatus, 1)
                .lt(SeckillActivity::getStartTime, now)
                .gt(SeckillActivity::getEndTime, now) // 还没结束
                .list();

        for (SeckillActivity activity : toStart) {
            try {
                activity.setStatus(2);
                activityService.updateById(activity);
                activityService.preheatStock(activity.getId());
                searchService.syncActivity(activity);
                log.info("活动状态流转：上架→进行中 activityId={}, name={}", activity.getId(), activity.getName());
            } catch (Exception e) {
                log.error("活动状态流转失败（上架→进行中）activityId={}", activity.getId(), e);
            }
        }

        // === 2. 进行中 → 已结束 ===
        List<SeckillActivity> toEnd = activityService.lambdaQuery()
                .eq(SeckillActivity::getStatus, 2)
                .lt(SeckillActivity::getEndTime, now)
                .list();

        for (SeckillActivity activity : toEnd) {
            try {
                activity.setStatus(3);
                activityService.updateById(activity);
                searchService.syncActivity(activity);
                clearRedisCache(activity.getId());
                log.info("活动状态流转：进行中→已结束 activityId={}, name={}", activity.getId(), activity.getName());
            } catch (Exception e) {
                log.error("活动状态流转失败（进行中→已结束）activityId={}", activity.getId(), e);
            }
        }
    }

    /**
     * 清理活动相关的所有 Redis 缓存
     */
    private void clearRedisCache(Long activityId) {
        String stockKey = STOCK_PREFIX + activityId;
        String soldKey = SOLD_PREFIX + activityId;
        String usersKey = USER_SET_PREFIX + activityId;
        String lockKey = LOCK_PREFIX + activityId;

        redisTemplate.delete(stockKey);
        redisTemplate.delete(soldKey);
        redisTemplate.delete(usersKey);
        redisTemplate.delete(lockKey);

        // 使用 SCAN 替代 KEYS 通配符删除，避免阻塞
        scanAndDelete(USER_COUNT_PREFIX + activityId + ":*");
        scanAndDelete(RESULT_PREFIX + activityId + ":*");

        log.debug("已清理活动 Redis 缓存 activityId={}", activityId);
    }

    /**
     * 使用 SCAN 模糊匹配删除 key，避免 KEYS 阻塞
     */
    private void scanAndDelete(String pattern) {
        try {
            redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                org.springframework.data.redis.core.ScanOptions options = org.springframework.data.redis.core.ScanOptions.scanOptions()
                        .match(pattern).count(100).build();
                org.springframework.data.redis.core.Cursor<byte[]> cursor = connection.scan(options);
                while (cursor.hasNext()) {
                    connection.del(cursor.next());
                }
                cursor.close();
                return null;
            });
        } catch (Exception e) {
            log.warn("SCAN 删除失败 pattern={}", pattern, e);
        }
    }
}
