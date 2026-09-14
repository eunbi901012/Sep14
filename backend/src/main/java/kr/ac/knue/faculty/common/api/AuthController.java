package kr.ac.knue.faculty.common.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.ac.knue.faculty.common.auth.AuthService;
import kr.ac.knue.faculty.common.model.ApiEnvelope;
import kr.ac.knue.faculty.common.model.Requests;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/login")
    public ApiEnvelope<?> login(@RequestBody Requests.LoginRequest request, HttpServletResponse response) {
        return ApiEnvelope.ok(authService.login(request, response));
    }

    @PostMapping("/auth/logout")
    public ApiEnvelope<?> logout(HttpServletRequest request, HttpServletResponse response) {
        return ApiEnvelope.ok(authService.logout(request, response));
    }

    @GetMapping("/auth/me")
    public ApiEnvelope<?> me(HttpServletRequest request) {
        return ApiEnvelope.ok(authService.currentUser(request));
    }

    @GetMapping("/health")
    public ApiEnvelope<?> health() {
        return ApiEnvelope.ok(java.util.Map.of("status", "UP"));
    }
}
