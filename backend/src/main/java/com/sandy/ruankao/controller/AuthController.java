package com.sandy.ruankao.controller;

import com.sandy.ruankao.common.ApiResponse;
import com.sandy.ruankao.config.AuthInterceptor;
import com.sandy.ruankao.dto.ChangePasswordRequest;
import com.sandy.ruankao.dto.LoginRequest;
import com.sandy.ruankao.dto.RegisterRequest;
import com.sandy.ruankao.entity.SysUser;
import com.sandy.ruankao.service.AuthService;
import java.util.*;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 用户注册、登录、退出及当前用户查询接口。
 *
 * @author zoujunke
 */
@RestController
@RequestMapping("/auth")
public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  public ApiResponse<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
    SysUser user =
        authService.register(request.getUsername(), request.getPassword(), request.getNickname());
    Map<String, Object> result = new LinkedHashMap<String, Object>();
    result.put("userId", user.getId());
    result.put("username", user.getUsername());
    result.put("nickname", user.getNickname());
    return ApiResponse.ok("注册成功，请登录", result);
  }

  @PostMapping("/login")
  public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
    return ApiResponse.ok("登录成功", authService.login(request.getUsername(), request.getPassword()));
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout() {
    // JWT 为无状态令牌，退出时由前端删除本地 Token，无需清理服务端会话。
    return ApiResponse.ok("已退出登录", null);
  }

  @GetMapping("/me")
  public ApiResponse<Map<String, Object>> me(
      @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthService.UserSession session) {
    return ApiResponse.ok(authService.profileResult(session));
  }

  @PutMapping("/password")
  public ApiResponse<Void> changePassword(
      @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthService.UserSession session,
      @Valid @RequestBody ChangePasswordRequest request) {
    authService.changePassword(
        session.getUserId(), request.getOldPassword(), request.getNewPassword());
    return ApiResponse.ok("密码修改成功，请重新登录", null);
  }
}
