package com.lightningdeal.search.repository;

import com.lightningdeal.search.entity.ActivityIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ES 仓库
 */
@Repository
public interface ActivitySearchRepository extends ElasticsearchRepository<ActivityIndex, Long> {

    /**
     * 根据名称模糊搜索
     */
    List<ActivityIndex> findByNameContainingOrGoodsNameContaining(String name, String goodsName);
}
