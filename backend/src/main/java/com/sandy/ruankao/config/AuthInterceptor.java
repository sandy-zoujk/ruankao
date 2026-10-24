package com.sandy.ruankao.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sandy.ruankao.common.ApiResponse;
import com.sandy.ruankao.service.AuthService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.*;
import java.nio.charset.StandardCharsets;

/**
 * 接口登录状态拦截器。
 *
 * @author zoujunke
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {
    public static final String CURRENT_USER_ATTRIBUTE = "currentUser";

    private final AuthService authService;
    private final ObjectMapper objectMapper;
    public AuthInterceptor(AuthService authService, ObjectMapper objectMapper) {
        this.authService = authService; this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 跨域预检请求不携带业务令牌，应直接放行，避免浏览器请求被提前拦截。
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        String authorization = request.getHeader("Authorization");
        // 仅接受标准的 Bearer Token，防止把完整 Authorization 请求头误当成令牌。
        String token = authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : null;
        AuthService.UserSession session = authService.authenticateToken(token);
        if (session != null) {
            request.setAttribute(CURRENT_USER_ATTRIBUTE, session);
            return true;
        }

        // 统一输出 JSON 格式的 401 响应，便于前端清除失效 JWT 并跳转登录页。
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error("登录已过期或Token无效")));
        return false;
    }
}
