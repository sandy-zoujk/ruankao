package com.sandy.ruankao.controller;

import com.sandy.ruankao.common.ApiResponse;
import com.sandy.ruankao.service.QuestionBankService;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/questions")
/** @author zoujunke */
public class AdminQuestionController {
  private final QuestionBankService service;

  public AdminQuestionController(QuestionBankService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<Map<String, Object>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String chapter,
      @RequestParam(required = false) Boolean enabled,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.ok(service.list(keyword, chapter, enabled, page, size));
  }

  @PostMapping
  public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
    return ApiResponse.ok("题目已创建", service.save(null, body));
  }

  @PutMapping("/{id}")
  public ApiResponse<Map<String, Object>> update(
      @PathVariable Long id, @RequestBody Map<String, Object> body) {
    return ApiResponse.ok("题目已更新", service.save(id, body));
  }

  @PatchMapping("/{id}/status")
  public ApiResponse<Void> status(@PathVariable Long id, @RequestBody Map<String, Object> body) {
    service.setEnabled(id, Boolean.parseBoolean(String.valueOf(body.get("enabled"))));
    return ApiResponse.ok("状态已更新", null);
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.ok("题目已删除或停用", null);
  }

  @GetMapping("/template")
  public ResponseEntity<byte[]> template() throws Exception {
    HttpHeaders h = new HttpHeaders();
    h.setContentType(
        MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    h.setContentDisposition(
        ContentDisposition.attachment()
            .filename("question-bank-template.xlsx", StandardCharsets.UTF_8)
            .build());
    return new ResponseEntity<>(service.template(), h, HttpStatus.OK);
  }

  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<Map<String, Object>> importExcel(@RequestPart("file") MultipartFile file)
      throws Exception {
    return ApiResponse.ok("导入完成", service.importExcel(file));
  }
}
