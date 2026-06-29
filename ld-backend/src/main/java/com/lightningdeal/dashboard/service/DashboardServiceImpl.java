package com.lightningdeal.dashboard.service;

import com.lightningdeal.dashboard.model.DashboardData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 大屏数据服务实现
 */
@Slf4j
@Service
public class DashboardServiceImpl implements DashboardService {

    private static final String QPS_KEY = "dashboard:qps:";
    private static final String TOTAL_ORDERS = "dashboard:total_orders";
    private static final String SUCCESS_ORDERS = "dashboard:success_orders";
    private static final String FAIL_ORDERS = "dashboard:fail_orders";

    /** 近60秒的 QPS 记录 */
    private final long[] qpsRingBuffer = new long[60];
    private int qpsIndex = 0;

    /** 实时抢购流水（最多保留100条） */
    private final List<DashboardData.FlashItem> flashStream = new CopyOnWriteArrayList<>();

    /** 商品销售统计 */
    private final Map<String, AtomicLong> goodsSales = new HashMap<>();

    private final RedisTemplate<String, Object> redisTemplate;

    public DashboardServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        // 初始化 QPS 数组
        Arrays.fill(qpsRingBuffer, 0);
    }

    @Override
    public DashboardData getDashboardData() {
        long currentQps = qpsRingBuffer[(qpsIndex - 1 + 60) % 60];
        long peakQps = Arrays.stream(qpsRingBuffer).max().orElse(0);

        // 构建排行榜
        List<DashboardData.RankItem> rankList = goodsSales.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().get(), a.getValue().get()))
                .limit(10)
                .map(entry -> DashboardData.RankItem.builder()
                        .goodsName(entry.getKey())
                        .salesCount(entry.getValue().get())
                        .rank(0)
                        .build())
                .collect(Collectors.toList());

        // 设置排名
        for (int i = 0; i < rankList.size(); i++) {
            rankList.get(i).setRank(i + 1);
        }

        long totalOrders = getLong(TOTAL_ORDERS);
        long successOrders = getLong(SUCCESS_ORDERS);
        long failOrders = getLong(FAIL_ORDERS);
        long remainStock = getLong("seckill:sold:1"); // 简化示例

        return DashboardData.builder()
                .currentQps(currentQps)
                .peakQps(peakQps)
                .totalOrders(totalOrders)
                .successOrders(successOrders)
                .failOrders(failOrders)
                .totalPreloadStock(1000)
                .remainStock((int) remainStock)
                .qpsHistory(Arrays.stream(qpsRingBuffer).boxed().collect(Collectors.toList()))
                .flashStream(new ArrayList<>(flashStream))
                .rankList(rankList)
                .systemStatus("🟢 正常")
                .build();
    }

    @Override
    public void recordFlash(boolean success, String goodsName, String username) {
        // 增加订单统计
        redisTemplate.opsForValue().increment(TOTAL_ORDERS);
        if (success) {
            redisTemplate.opsForValue().increment(SUCCESS_ORDERS);
        } else {
            redisTemplate.opsForValue().increment(FAIL_ORDERS);
        }

        // 记录抢购流水
        DashboardData.FlashItem item = DashboardData.FlashItem.builder()
                .username(maskUsername(username))
                .success(success)
                .goodsName(goodsName)
                .timestamp(System.currentTimeMillis())
                .build();
        flashStream.add(0, item);
        if (flashStream.size() > 100) {
            flashStream.remove(flashStream.size() - 1);
        }

        // 商品销售统计
        goodsSales.computeIfAbsent(goodsName, k -> new AtomicLong()).incrementAndGet();
    }

    @Override
    public long getCurrentQps() {
        return qpsRingBuffer[(qpsIndex - 1 + 60) % 60];
    }

    /**
     * 每秒统计一次 QPS
     */
    @Scheduled(fixedRate = 1000)
    public void recordQps() {
        // 统计上一秒的请求数（简化实现，实际可从 Redis 计数器读取）
        String key = QPS_KEY + System.currentTimeMillis() / 1000;
        Object val = redisTemplate.opsForValue().get(key);
        long count = val instanceof Number ? ((Number) val).longValue() : 0;

        qpsRingBuffer[qpsIndex] = count;
        qpsIndex = (qpsIndex + 1) % 60;

        // 清理旧的 key
        redisTemplate.delete(QPS_KEY + (System.currentTimeMillis() / 1000 - 120));
    }

    private long getLong(String key) {
        Object val = redisTemplate.opsForValue().get(key);
        return val instanceof Number ? ((Number) val).longValue() : 0;
    }

    /**
     * 脱敏用户名
     */
    private String maskUsername(String username) {
        if (username == null || username.length() < 2) return username;
        return username.substring(0, 1) + "***" + username.substring(username.length() - 1);
    }
}
