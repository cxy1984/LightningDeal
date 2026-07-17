package com.lightningdeal.common.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * R（统一响应体）单元测试
 *
 * 测试目标：
 * 1. 各种工厂方法正确创建响应对象
 * 2. Jackson 序列化/反序列化正常
 */
class RTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateOkResponse() {
        R<String> r = R.ok("hello");

        assertThat(r.getCode()).isEqualTo(200);
        assertThat(r.getMsg()).isEqualTo("success");
        assertThat(r.getData()).isEqualTo("hello");
    }

    @Test
    void shouldCreateOkResponseWithoutData() {
        R<Void> r = R.ok();

        assertThat(r.getCode()).isEqualTo(200);
        assertThat(r.getMsg()).isEqualTo("success");
        assertThat(r.getData()).isNull();
    }

    @Test
    void shouldCreateOkResponseWithCustomMsg() {
        R<Integer> r = R.ok("操作成功", 42);

        assertThat(r.getCode()).isEqualTo(200);
        assertThat(r.getMsg()).isEqualTo("操作成功");
        assertThat(r.getData()).isEqualTo(42);
    }

    @Test
    void shouldCreateFailResponseWithCode() {
        R<Void> r = R.fail(400, "参数错误");

        assertThat(r.getCode()).isEqualTo(400);
        assertThat(r.getMsg()).isEqualTo("参数错误");
        assertThat(r.getData()).isNull();
    }

    @Test
    void shouldCreateFailResponseWithDefaultCode() {
        R<Void> r = R.fail("服务器内部错误");

        assertThat(r.getCode()).isEqualTo(500);
        assertThat(r.getMsg()).isEqualTo("服务器内部错误");
        assertThat(r.getData()).isNull();
    }

    @Test
    void shouldCreateFailResponseWithoutMsg() {
        R<Void> r = R.fail();

        assertThat(r.getCode()).isEqualTo(500);
        assertThat(r.getMsg()).isEqualTo("Internal Server Error");
        assertThat(r.getData()).isNull();
    }

    @Test
    void shouldCreateBadRequestResponse() {
        R<Void> r = R.badRequest("输入不合法");

        assertThat(r.getCode()).isEqualTo(400);
        assertThat(r.getMsg()).isEqualTo("输入不合法");
    }

    @Test
    void shouldCreateUnauthorizedResponse() {
        R<Void> r = R.unauthorized("请先登录");

        assertThat(r.getCode()).isEqualTo(401);
        assertThat(r.getMsg()).isEqualTo("请先登录");
    }

    @Test
    void shouldCreateForbiddenResponse() {
        R<Void> r = R.forbidden("权限不足");

        assertThat(r.getCode()).isEqualTo(403);
        assertThat(r.getMsg()).isEqualTo("权限不足");
    }

    @Test
    void shouldSerializeToJsonAndBack() throws Exception {
        R<String> original = R.ok("test-data");

        String json = objectMapper.writeValueAsString(original);
        @SuppressWarnings("unchecked")
        R<String> deserialized = objectMapper.readValue(json, R.class);

        assertThat(deserialized.getCode()).isEqualTo(200);
        assertThat(deserialized.getMsg()).isEqualTo("success");
        assertThat(deserialized.getData()).isEqualTo("test-data");
    }

    @Test
    void shouldSerializeFailResponseToJsonAndBack() throws Exception {
        R<Void> original = R.fail(429, "限流了");

        String json = objectMapper.writeValueAsString(original);
        R<Void> deserialized = objectMapper.readValue(json, R.class);

        assertThat(deserialized.getCode()).isEqualTo(429);
        assertThat(deserialized.getMsg()).isEqualTo("限流了");
    }

    @Test
    void okResponseShouldHaveSerialVersionUid() {
        // 验证 R 类实现了 Serializable
        assertThat(R.class.getInterfaces())
                .contains(java.io.Serializable.class);
    }
}
