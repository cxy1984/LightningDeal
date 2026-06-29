package com.lightningdeal.search.service;

import com.lightningdeal.activity.entity.SeckillActivity;
import com.lightningdeal.search.entity.ActivityIndex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * ES 搜索服务
 */
public interface SearchService {

    /**
     * 同步活动数据到 ES
     */
    void syncActivity(SeckillActivity activity);

    /**
     * 批量同步
     */
    void syncActivities(List<SeckillActivity> activities);

    /**
     * 搜索活动（ik 分词 + 高亮）
     */
    Page<ActivityIndex> search(String keyword, Pageable pageable);

    /**
     * 删除索引
     */
    void deleteIndex(Long activityId);
}
