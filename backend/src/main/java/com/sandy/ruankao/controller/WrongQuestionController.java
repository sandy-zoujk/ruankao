package com.sandy.ruankao.controller;

import com.sandy.ruankao.common.ApiResponse;
import com.sandy.ruankao.config.AuthInterceptor;
import com.sandy.ruankao.entity.KnowledgeChapter;
import com.sandy.ruankao.entity.WrongQuestion;
import com.sandy.ruankao.mapper.KnowledgeChapterMapper;
import com.sandy.ruankao.mapper.WrongQuestionMapper;
import com.sandy.ruankao.service.AuthService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 错题记录、复习状态及删除接口。
 *
 * @author zoujunke
 */
@RestController
@RequestMapping("/wrong-questions")
public class WrongQuestionController {
  private final WrongQuestionMapper mapper;
  private final KnowledgeChapterMapper chapterMapper;

  public WrongQuestionController(WrongQuestionMapper mapper, KnowledgeChapterMapper chapterMapper) {
    this.mapper = mapper;
    this.chapterMapper = chapterMapper;
  }

  @GetMapping
  public ApiResponse<List<WrongQuestion>> list(
      @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthService.UserSession session) {
    return ApiResponse.ok(mapper.findAll(session.getUserId()));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<WrongQuestion>> create(
      @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthService.UserSession session,
      @Valid @RequestBody WrongQuestion question,
      @RequestParam(required = false) Long chapterId) {
    question.setId(null);
    question.setUserId(session.getUserId());
    if (chapterId != null) {
      // 章节对象必须从数据库读取，不能直接信任前端传入的嵌套对象。
      KnowledgeChapter chapter = chapterMapper.findById(chapterId, session.getUserId());
      if (chapter == null) throw new IllegalArgumentException("章节不存在或无权访问");
      question.setChapter(chapter);
    }
    if (question.getChapter() == null) question.setChapter(new KnowledgeChapter());
    // 未指定复习日期时默认次日复习，确保新错题及时进入复习队列。
    if (question.getNextReviewDate() == null)
      question.setNextReviewDate(LocalDate.now().plusDays(1));
    mapper.insert(question);
    return ResponseEntity.ok(
        ApiResponse.ok("错题已保存", mapper.findById(question.getId(), session.getUserId())));
  }

  @PatchMapping("/{id}/review")
  public ResponseEntity<ApiResponse<WrongQuestion>> review(
      @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE) AuthService.UserSession session,
      @PathVariable Long id,
      @RequestBody Map<String, Boolean> body) {
    WrongQuestion question = mapper.findById(id, session.getUserId());
    if (question == null) return ResponseEntity.notFound().build();
    // 已掌握的错题不再安排复习；未掌握的错题三天后再次出现。
    boolean mastered = Boolean.TRUE.equals(body.get("mastered"));
    question.setMastered(mastered);
    question.setReviewCount(question.getReviewCount() + 1);
    question.setNextReviewDate(mastered ? null : LocalDate.now().plusDays(3));
    mapper.updateReview(question);
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
