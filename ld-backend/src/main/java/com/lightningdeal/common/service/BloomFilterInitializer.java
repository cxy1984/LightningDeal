package com.lightningdeal.common.service;

import com.lightningdeal.activity.mapper.SeckillActivityMapper;
import com.lightningdeal.activity.entity.SeckillActivity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 布隆过滤器数据初始化
 *
 * <p>应用启动后，将所有未被逻辑删除的活动 ID 加载到布隆过滤器中。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BloomFilterInitializer implements CommandLineRunner {

    private final BloomFilterService bloomFilterService;
    private final SeckillActivityMapper activityMapper;

    @Override
    public void run(String... args) {
        try {
            // 查询所有有效活动（deleted=0）
            List<SeckillActivity> activities = activityMapper.selectList(null);
            List<Long> ids = activities.stream()
                    .map(SeckillActivity::getId)
                    .collect(Collectors.toList());

            if (ids.isEmpty()) {
                log.info("布隆过滤器初始化：数据库中没有活动数据，跳过加载");
                return;
            }

            // 批量添加到布隆过滤器
            bloomFilterService.addAll(ids);

            log.info("布隆过滤器初始化完成：共加载 {} 个活动 ID，过滤器当前计数={}",
                    ids.size(), bloomFilterService.count());
        } catch (Exception e) {
            log.error("布隆过滤器初始化失败，但不影响系统启动", e);
        }
    }
}
