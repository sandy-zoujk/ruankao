package com.sandy.ruankao.controller;

import com.sandy.ruankao.common.ApiResponse;
import com.sandy.ruankao.config.AuthInterceptor;
import com.sandy.ruankao.entity.KnowledgeChapter;
import com.sandy.ruankao.mapper.KnowledgeChapterMapper;
import com.sandy.ruankao.service.AuthService;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 知识章节及掌握度维护接口。
 *
 * @author zoujunke
 */
@RestController
@RequestMapping("/chapters")
public class KnowledgeChapterController {
  private final KnowledgeChapterMapper mapper;

  public KnowledgeChapterController(KnowledgeChapterMapper mapper) {
    this.mapper = mapper;
  }

  @GetMapping
  public ApiResponse<Map<String, Object>> list(
      @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthService.UserSession session,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size) {
    page = Math.max(1, page);
    size = Math.max(1, Math.min(100, size));
    Map<String, Object> result = new LinkedHashMap<String, Object>();
    result.put("items", mapper.findPage(session.getUserId(), size, (long) (page - 1) * size));
    result.put("total", mapper.count(session.getUserId()));
    result.put("page", page);
    result.put("size", size);
    return ApiResponse.ok(result);
  }

  @GetMapping("/options")
  public ApiResponse<List<KnowledgeChapter>> options(
      @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthService.UserSession session) {
    return ApiResponse.ok(mapper.findAll(session.getUserId()));
  }

  @PostMapping
  public ApiResponse<KnowledgeChapter> create(
      @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthService.UserSession session,
      @Valid @RequestBody KnowledgeChapter chapter) {
    chapter.setId(null);
    chapter.setUserId(session.getUserId());
    mapper.insert(chapter);
    return ApiResponse.ok(mapper.findById(chapter.getId(), session.getUserId()));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<KnowledgeChapter>> update(
      @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthService.UserSession session,
      @PathVariable Long id,
      @Valid @RequestBody KnowledgeChapter input) {
    KnowledgeChapter chapter = mapper.findById(id, session.getUserId());
    if (chapter == null) return ResponseEntity.notFound().build();
    // 仅复制允许修改的业务字段，防止请求体覆盖数据库主键。
    chapter.setName(input.getName());
    chapter.setCategory(input.getCategory());
    chapter.setPriority(input.getPriority());
    chapter.setMastery(input.getMastery());
    chapter.setQuestionCount(input.getQuestionCount());
    chapter.setCorrectCount(input.getCorrectCount());
    // 每次更新掌握情况时同步记录最后复习日期。
    chapter.setLastReviewDate(LocalDate.now());
    mapper.update(chapter);
    return ResponseEntity.ok(ApiResponse.ok(mapper.findById(id, session.getUserId())));
  }
}
