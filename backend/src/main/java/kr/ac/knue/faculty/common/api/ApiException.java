package kr.ac.knue.faculty.common.api;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final String denialStep;

    public ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, null);
    }

    public ApiException(HttpStatus status, String code, String message, String denialStep) {
        super(message);
        this.status = status;
        this.code = code;
        this.denialStep = denialStep;
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }
    public String denialStep() { return denialStep; }
}
