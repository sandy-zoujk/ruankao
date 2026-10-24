package com.sandy.ruankao.controller;

import com.sandy.ruankao.common.ApiResponse;
import com.sandy.ruankao.config.AuthInterceptor;
import com.sandy.ruankao.entity.StudyPlan;
import com.sandy.ruankao.mapper.StudyPlanMapper;
import com.sandy.ruankao.service.AuthService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 学习计划的新增、查询、修改和删除接口。
 *
 * @author zoujunke
 */
@RestController
@RequestMapping("/plans")
public class StudyPlanController {
  private final StudyPlanMapper mapper;

  public StudyPlanController(StudyPlanMapper mapper) {
    this.mapper = mapper;
  }

  @GetMapping
  public ApiResponse<List<StudyPlan>> list(
      @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthService.UserSession session) {
    return ApiResponse.ok(mapper.findAll(session.getUserId()));
  }

  @PostMapping
  public ApiResponse<StudyPlan> create(
      @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthService.UserSession session,
      @Valid @RequestBody StudyPlan plan) {
    plan.setId(null);
    plan.setUserId(session.getUserId());
    mapper.insert(plan);
    return ApiResponse.ok("计划已创建", mapper.findById(plan.getId(), session.getUserId()));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<StudyPlan>> update(
      @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthService.UserSession session,
      @PathVariable Long id,
      @Valid @RequestBody StudyPlan input) {
    StudyPlan plan = mapper.findById(id, session.getUserId());
    if (plan == null) return ResponseEntity.notFound().build();
    // 只更新前端可维护字段，主键和创建时间继续使用数据库原值。
    plan.setTitle(input.getTitle());
    plan.setCategory(input.getCategory());
    plan.setPlanDate(input.getPlanDate());
    plan.setDurationMinutes(input.getDurationMinutes());
    plan.setCompleted(input.getCompleted());
    plan.setNotes(input.getNotes());
    mapper.update(plan);
    return ResponseEntity.ok(ApiResponse.ok("计划已更新", mapper.findById(id, session.getUserId())));
  }

  @PatchMapping("/{id}/toggle")
  public ResponseEntity<ApiResponse<StudyPlan>> toggle(
      @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthService.UserSession session,
      @PathVariable Long id) {
    if (mapper.findById(id, session.getUserId()) == null) return ResponseEntity.notFound().build();
    // 完成状态在 SQL 中原子取反，减少并发请求造成的状态覆盖。
    mapper.toggle(id, session.getUserId());
    return ResponseEntity.ok(ApiResponse.ok(mapper.findById(id, session.getUserId())));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(
      @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthService.UserSession session,
      @PathVariable Long id) {
    if (mapper.delete(id, session.getUserId()) == 0) return ResponseEntity.notFound().build();
    return ResponseEntity.noContent().build();
  }
}
