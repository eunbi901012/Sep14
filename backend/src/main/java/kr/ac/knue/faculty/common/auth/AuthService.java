package kr.ac.knue.faculty.common.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.ac.knue.faculty.common.api.ApiException;
import kr.ac.knue.faculty.common.mapper.CommonMapper;
import kr.ac.knue.faculty.common.model.Requests.LoginRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    public static final String COOKIE_NAME = "APPSESSION";
    private final CommonMapper mapper;

    public AuthService(CommonMapper mapper) {
        this.mapper = mapper;
    }

    public Map<String, Object> login(LoginRequest request, HttpServletResponse response) {
        if (request == null || blank(request.loginId()) || blank(request.password())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "아이디와 password는 필수입니다.");
        }
        Map<String, Object> user = mapper.findLoginUser(request.loginId());
        if (user == null || !request.password().equals(String.valueOf(user.get("passwordHash"))) || !"Y".equals(String.valueOf(user.get("useYn")))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인 정보가 올바르지 않습니다.");
        }
        String sessionId = UUID.randomUUID().toString();
        String userId = String.valueOf(user.get("userId"));
        mapper.createSession(sessionId, userId, LocalDateTime.now().plusHours(8));
        Cookie cookie = new Cookie(COOKIE_NAME, sessionId);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
        return Map.of(
            "authenticated", true,
            "userId", userId,
            "loginId", user.get("loginId"),
            "roleCodes", mapper.roleCodes(userId),
            "defaultRoute", "/admin/users"
        );
    }

    public Map<String, Object> logout(HttpServletRequest request, HttpServletResponse response) {
        String sessionId = cookie(request);
        if (sessionId != null) mapper.logout(sessionId);
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
        return Map.of("loggedOut", true);
    }

    public Map<String, Object> requireSession(HttpServletRequest request) {
        String sessionId = cookie(request);
        if (sessionId == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증 세션이 필요합니다.");
        Map<String, Object> session = mapper.findSession(sessionId);
        if (session == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증 세션이 만료되었습니다.");
        return session;
    }

    public Map<String, Object> currentUser(HttpServletRequest request) {
        Map<String, Object> session = requireSession(request);
        String userId = String.valueOf(session.get("userId"));
        return Map.of(
            "userId", userId,
            "loginId", session.get("loginId"),
            "roleCodes", mapper.roleCodes(userId),
            "menuPaths", mapper.menuPaths(userId)
        );
    }

    public void requireAdmin(HttpServletRequest request) {
        Map<String, Object> session = requireSession(request);
        List<String> roles = mapper.roleCodes(String.valueOf(session.get("userId")));
        if (!roles.contains("R09")) throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "R09 권한이 필요합니다.");
    }

    private String cookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName()) && !blank(cookie.getValue())) return cookie.getValue();
        }
        return null;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
