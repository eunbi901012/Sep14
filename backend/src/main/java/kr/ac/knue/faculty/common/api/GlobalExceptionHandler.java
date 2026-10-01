package kr.ac.knue.faculty.common.api;

import kr.ac.knue.faculty.common.model.ApiEnvelope;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiEnvelope<?>> api(ApiException ex) {
        Map<String, Object> meta = ex.denialStep() == null ? Map.of() : Map.of("denialStep", ex.denialStep());
        return ResponseEntity.status(ex.status()).body(ApiEnvelope.error(ex.code(), ex.getMessage(), meta));
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ResponseEntity<ApiEnvelope<?>> badRequest(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiEnvelope.error("BAD_REQUEST", ex.getMessage()));
    }
}
