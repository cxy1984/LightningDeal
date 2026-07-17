package com.lightningdeal.seckill.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SeckillResult 单元测试
 *
 * 测试目标：
 * 1. 工厂方法创建正确状态的结果对象
 * 2. Jackson 序列化/反序列化不出错（用于 Redis 缓存和 WebSocket 推送）
 */
class SeckillResultTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateQueuingResult() {
        SeckillResult result = SeckillResult.queuing(1L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("排队中");
        assertThat(result.getActivityId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(SeckillResult.STATUS_QUEUING);
        assertThat(result.getOrderId()).isNull();
    }

    @Test
    void shouldCreateSuccessResult() {
        SeckillResult result = SeckillResult.success(1L, 100L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("抢购成功！");
        assertThat(result.getActivityId()).isEqualTo(1L);
        assertThat(result.getOrderId()).isEqualTo(100L);
        assertThat(result.getStatus()).isEqualTo(SeckillResult.STATUS_SUCCESS);
    }

    @Test
    void shouldCreateFailResult() {
        SeckillResult result = SeckillResult.fail("手慢啦，库存已被抢完", 2L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("手慢啦，库存已被抢完");
        assertThat(result.getActivityId()).isEqualTo(2L);
        assertThat(result.getStatus()).isEqualTo(SeckillResult.STATUS_FAIL);
        assertThat(result.getOrderId()).isNull();
    }

    @Test
    void shouldCreateRepeatResult() {
        SeckillResult result = SeckillResult.repeat(3L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("您已参与过该活动");
        assertThat(result.getActivityId()).isEqualTo(3L);
        assertThat(result.getStatus()).isEqualTo(SeckillResult.STATUS_REPEAT);
        assertThat(result.getOrderId()).isNull();
    }

    @Test
    void shouldSerializeToJsonAndBack() throws Exception {
        SeckillResult original = SeckillResult.success(1L, 100L);

        String json = objectMapper.writeValueAsString(original);
        SeckillResult deserialized = objectMapper.readValue(json, SeckillResult.class);

        assertThat(deserialized.isSuccess()).isTrue();
        assertThat(deserialized.getMessage()).isEqualTo("抢购成功！");
        assertThat(deserialized.getActivityId()).isEqualTo(1L);
        assertThat(deserialized.getOrderId()).isEqualTo(100L);
        assertThat(deserialized.getStatus()).isEqualTo(SeckillResult.STATUS_SUCCESS);
    }

    @Test
    void shouldSerializeFailResultToJsonAndBack() throws Exception {
        SeckillResult original = SeckillResult.fail("库存不足", 2L);

        String json = objectMapper.writeValueAsString(original);
        SeckillResult deserialized = objectMapper.readValue(json, SeckillResult.class);

        assertThat(deserialized.isSuccess()).isFalse();
        assertThat(deserialized.getMessage()).isEqualTo("库存不足");
        assertThat(deserialized.getActivityId()).isEqualTo(2L);
        assertThat(deserialized.getStatus()).isEqualTo(SeckillResult.STATUS_FAIL);
    }

    @Test
    void shouldSerializeQueuingResultToJsonAndBack() throws Exception {
        SeckillResult original = SeckillResult.queuing(1L);

        String json = objectMapper.writeValueAsString(original);
        SeckillResult deserialized = objectMapper.readValue(json, SeckillResult.class);

        assertThat(deserialized.isSuccess()).isTrue();
        assertThat(deserialized.getMessage()).isEqualTo("排队中");
        assertThat(deserialized.getActivityId()).isEqualTo(1L);
        assertThat(deserialized.getStatus()).isEqualTo(SeckillResult.STATUS_QUEUING);
    }

    @Test
    void statusConstantsShouldBeDistinct() {
        assertThat(SeckillResult.STATUS_QUEUING).isEqualTo(1);
        assertThat(SeckillResult.STATUS_SUCCESS).isEqualTo(2);
        assertThat(SeckillResult.STATUS_FAIL).isEqualTo(3);
        assertThat(SeckillResult.STATUS_REPEAT).isEqualTo(4);
    }
}
