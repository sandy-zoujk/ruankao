package com.sandy.ruankao.controller;

import com.sandy.ruankao.common.ApiResponse;
import com.sandy.ruankao.config.AuthInterceptor;
import com.sandy.ruankao.service.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tutorials")
/** @author zoujunke */
public class TutorialController {
  private final TutorialService service;

  public TutorialController(TutorialService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<List<Map<String, Object>>> list(
      @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthService.UserSession u,
      @RequestParam(required = false) String chapter) {
    return ApiResponse.ok(service.catalog(u.getUserId(), chapter));
  }

  @GetMapping("/progress")
  public ApiResponse<Map<String, Object>> progress(
      @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthService.UserSession u) {
    return ApiResponse.ok(service.progress(u.getUserId()));
  }

  @GetMapping("/{sourceId}")
  public ApiResponse<Map<String, Object>> detail(
      @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthService.UserSession u,
      @PathVariable Long sourceId) {
    return ApiResponse.ok(service.detail(u.getUserId(), sourceId));
  }

  @PutMapping("/{sourceId}/sections/{sectionKey}/progress")
  public ApiResponse<Void> complete(
      @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthService.UserSession u,
      @PathVariable Long sourceId,
      @PathVariable String sectionKey,
      @RequestBody Map<String, Object> b) {
    service.setCompleted(
        u.getUserId(),
        sourceId,
        sectionKey,
        Boolean.parseBoolean(String.valueOf(b.get("completed"))));
    return ApiResponse.ok("学习进度已更新", null);
  }
}
