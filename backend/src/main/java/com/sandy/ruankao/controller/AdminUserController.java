package com.sandy.ruankao.controller;

import com.sandy.ruankao.common.ApiResponse;
import com.sandy.ruankao.config.AuthInterceptor;
import com.sandy.ruankao.dto.UserStatusRequest;
import com.sandy.ruankao.dto.UserSummaryResponse;
import com.sandy.ruankao.service.AuthService;
import com.sandy.ruankao.service.UserAdminService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

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
    public ApiResponse<List<UserSummaryResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.ok(userAdminService.list(keyword, enabled));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<UserSummaryResponse> updateStatus(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthService.UserSession session,
            @PathVariable Long id,
            @Valid @RequestBody UserStatusRequest request) {
        return ApiResponse.ok("账号状态已更新",
                userAdminService.updateStatus(session.getUserId(), id, request.getEnabled()));
    }
}
