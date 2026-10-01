package kr.ac.knue.faculty.common.model;

import java.util.Map;

public record ApiEnvelope<T>(boolean success, T data, String message, Map<String, Object> meta, Object errors) {
    public ApiEnvelope(boolean success, T data, String message) {
        this(success, data, message, Map.of(), null);
    }

    public static <T> ApiEnvelope<T> ok(T data) {
        return new ApiEnvelope<>(true, data, "OK", Map.of("message", "OK"), null);
    }

    public static ApiEnvelope<Map<String, Object>> error(String code, String message) {
        return error(code, message, Map.of());
    }

    public static ApiEnvelope<Map<String, Object>> error(String code, String message, Map<String, Object> meta) {
        Map<String, Object> nextMeta = new java.util.LinkedHashMap<>(meta);
        nextMeta.putIfAbsent("code", code);
        nextMeta.putIfAbsent("message", message);
        return new ApiEnvelope<>(false, Map.of("code", code), message, nextMeta, null);
    }
}
