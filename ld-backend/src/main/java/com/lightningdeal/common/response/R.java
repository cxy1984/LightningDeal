package com.lightningdeal.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一响应体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统一响应体")
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "状态码")
    private int code;

    @Schema(description = "消息")
    private String msg;

    @Schema(description = "数据")
    private T data;

    @SuppressWarnings("unchecked")
    public static <T> R<T> ok() {
        return (R<T>) new R<>(200, "success", null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(200, "success", data);
    }

    public static <T> R<T> ok(String msg, T data) {
        return new R<>(200, msg, data);
    }

    @SuppressWarnings("unchecked")
    public static <T> R<T> fail(int code, String msg) {
        return (R<T>) new R<>(code, msg, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> R<T> fail(String msg) {
        return (R<T>) new R<>(500, msg, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> R<T> fail() {
        return (R<T>) new R<>(500, "Internal Server Error", null);
    }

    @SuppressWarnings("unchecked")
    public static <T> R<T> badRequest(String msg) {
        return (R<T>) new R<>(400, msg, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> R<T> unauthorized(String msg) {
        return (R<T>) new R<>(401, msg, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> R<T> forbidden(String msg) {
        return (R<T>) new R<>(403, msg, null);
    }
}
