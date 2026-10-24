package com.sandy.ruankao.controller;

import com.sandy.ruankao.common.ApiResponse;
import com.sandy.ruankao.service.TutorialService;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/tutorials")
/** @author zoujunke */
public class AdminTutorialController {
  private final TutorialService service;

  public AdminTutorialController(TutorialService service) {
    this.service = service;
  }

  @GetMapping("/sources")
  public ApiResponse<List<Map<String, Object>>> sources() {
    return ApiResponse.ok(service.adminSources());
  }

  @PostMapping("/sources")
  public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> b)
      throws Exception {
    return ApiResponse.ok("来源已添加", service.saveSource(null, b));
  }

  @PutMapping("/sources/{id}")
  public ApiResponse<Map<String, Object>> update(
      @PathVariable Long id, @RequestBody Map<String, Object> b) throws Exception {
    return ApiResponse.ok("来源已更新", service.saveSource(id, b));
  }

  @PostMapping("/sources/{id}/sync")
  public ApiResponse<Map<String, Object>> sync(@PathVariable Long id) {
    return ApiResponse.ok("同步完成", service.sync(id));
  }

  @GetMapping("/sources/{id}/versions")
  public ApiResponse<List<Map<String, Object>>> versions(@PathVariable Long id) {
    return ApiResponse.ok(service.versions(id));
  }

  @GetMapping("/sources/{id}/versions/{versionId}")
  public ApiResponse<Map<String, Object>> preview(
      @PathVariable Long id, @PathVariable Long versionId) {
    return ApiResponse.ok(service.preview(id, versionId));
  }

  @PostMapping("/sources/{id}/versions/{versionId}/publish")
  public ApiResponse<Void> publish(@PathVariable Long id, @PathVariable Long versionId) {
    service.publish(id, versionId);
    return ApiResponse.ok("教程已发布", null);
  }

  @PostMapping("/sources/{id}/unpublish")
  public ApiResponse<Void> unpublish(@PathVariable Long id) {
    service.unpublish(id);
    return ApiResponse.ok("教程已下架", null);
  }
}
