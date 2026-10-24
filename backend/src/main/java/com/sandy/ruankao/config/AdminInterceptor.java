package com.sandy.ruankao.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sandy.ruankao.common.ApiResponse;
import com.sandy.ruankao.entity.SysUser;
import com.sandy.ruankao.service.AuthService;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理员接口权限拦截器。
 *
 * @author zoujunke
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {
  private final ObjectMapper objectMapper;

  public AdminInterceptor(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    AuthService.UserSession session =
        (AuthService.UserSession) request.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
    if (session != null && SysUser.ROLE_ADMIN.equals(session.getRole())) return true;

    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error("无管理员权限")));
    return false;
  }
}
