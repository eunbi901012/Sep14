package kr.ac.knue.faculty.common.model;

import java.util.Map;

public record ApiEnvelope<T>(boolean success, T data, String message) {
    public static <T> ApiEnvelope<T> ok(T data) {
        return new ApiEnvelope<>(true, data, "OK");
    }

    public static ApiEnvelope<Map<String, Object>> error(String code, String message) {
        return new ApiEnvelope<>(false, Map.of("code", code), message);
    }
}
