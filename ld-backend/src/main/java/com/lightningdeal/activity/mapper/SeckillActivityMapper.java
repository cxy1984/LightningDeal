package com.lightningdeal.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lightningdeal.activity.entity.SeckillActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 秒杀活动 Mapper
 */
@Mapper
public interface SeckillActivityMapper extends BaseMapper<SeckillActivity> {

    /**
     * 乐观锁扣减库存
     */
    @Update("UPDATE seckill_activity SET sold_stock = sold_stock + 1 " +
            "WHERE id = #{activityId} AND sold_stock < total_stock")
    int decrementStock(@Param("activityId") Long activityId);

    /**
     * 取消订单时回退已售数量
     */
    @Update("UPDATE seckill_activity SET sold_stock = sold_stock - 1 " +
            "WHERE id = #{activityId} AND sold_stock > 0")
    int revertSoldStock(@Param("activityId") Long activityId);
}
