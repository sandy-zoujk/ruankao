package com.sandy.ruankao.controller;

import com.sandy.ruankao.common.ApiResponse;
import com.sandy.ruankao.config.AuthInterceptor;
import com.sandy.ruankao.dto.UserRoleRequest;
import com.sandy.ruankao.dto.UserStatusRequest;
import com.sandy.ruankao.dto.UserSummaryResponse;
import com.sandy.ruankao.service.AuthService;
import com.sandy.ruankao.service.UserAdminService;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员用户管理接口。
 *
 * @author zoujunke
 */
@RestController
@RequestMapping("/admin/users")
public class AdminUserController {
  private final UserAdminService userAdminService;

  public AdminUserController(UserAdminService userAdminService) {
    this.userAdminService = userAdminService;
  }

  @GetMapping
  public ApiResponse<Map<String, Object>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Boolean enabled,
      @RequestParam(required = false) String role,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.ok(userAdminService.list(keyword, enabled, role, page, size));
  }

  @PatchMapping("/{id}/status")
  public ApiResponse<UserSummaryResponse> updateStatus(
      @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthService.UserSession session,
      @PathVariable Long id,
      @Valid @RequestBody UserStatusRequest request) {
    return ApiResponse.ok(
        "账号状态已更新", userAdminService.updateStatus(session.getUserId(), id, request.getEnabled()));
  }

  @PatchMapping("/{id}/role")
  public ApiResponse<UserSummaryResponse> updateRole(
      @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthService.UserSession session,
      @PathVariable Long id,
      @Valid @RequestBody UserRoleRequest request) {
    return ApiResponse.ok(
        "用户角色已更新", userAdminService.updateRole(session.getUserId(), id, request.getRole()));
  }
}
