package com.sandy.ruankao.controller;

import com.sandy.ruankao.common.ApiResponse;
import com.sandy.ruankao.config.AuthInterceptor;
import com.sandy.ruankao.service.AuthService;
import com.sandy.ruankao.service.PracticeService;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/practice")
/** @author zoujunke */
public class PracticeController {
  private final PracticeService service;

  public PracticeController(PracticeService service) {
    this.service = service;
  }

  @GetMapping("/chapters")
  public ApiResponse<List<Map<String, Object>>> chapters() {
    return ApiResponse.ok(service.chapters());
  }

  @PostMapping("/attempts")
  public ApiResponse<Map<String, Object>> create(
      @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthService.UserSession user,
      @RequestBody Map<String, Object> body) {
    return ApiResponse.ok(
        service.create(
            user.getUserId(),
            Objects.toString(body.get("chapter"), null),
            Integer.parseInt(String.valueOf(body.getOrDefault("questionCount", 10)))));
  }

  @PostMapping("/attempts/{id}/submit")
  public ApiResponse<Map<String, Object>> submit(
      @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthService.UserSession user,
      @PathVariable Long id,
      @RequestBody Map<String, Object> body) {
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> answers = (List<Map<String, Object>>) body.get("answers");
    return ApiResponse.ok("交卷成功", service.submit(user.getUserId(), id, answers));
  }

  @GetMapping("/attempts/{id}")
  public ApiResponse<Map<String, Object>> detail(
      @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthService.UserSession user,
      @PathVariable Long id) {
    return ApiResponse.ok(service.detail(user.getUserId(), id, true));
  }

  @GetMapping("/history")
  public ApiResponse<List<Map<String, Object>>> history(
      @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthService.UserSession user) {
    return ApiResponse.ok(service.history(user.getUserId()));
  }

  @GetMapping("/stats")
  public ApiResponse<Map<String, Object>> stats(
      @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthService.UserSession user) {
    return ApiResponse.ok(service.stats(user.getUserId()));
  }
}
