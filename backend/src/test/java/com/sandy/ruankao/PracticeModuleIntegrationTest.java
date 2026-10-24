package com.sandy.ruankao;

import static org.junit.jupiter.api.Assertions.*;

import com.sandy.ruankao.service.PracticeService;
import com.sandy.ruankao.service.QuestionBankService;
import java.io.ByteArrayOutputStream;
import java.util.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class PracticeModuleIntegrationTest {
  @Autowired PracticeService practice;
  @Autowired QuestionBankService bank;
  @Autowired JdbcTemplate jdbc;

  @Test
  void exerciseGradesAndKeepsSubmissionIdempotentAndWrongQuestionUnique() {
    long userId = createUser("practice_user");
    jdbc.update(
        "INSERT INTO knowledge_chapter(user_id,name,category) VALUES(?,?,'综合知识')",
        userId,
        "项目整合管理");
    bank.save(null, question("PM-001", "A", "第一题"));
    bank.save(null, question("PM-002", "B", "第二题"));

    Map<String, Object> attempt = practice.create(userId, "项目整合管理", 2);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> questions = (List<Map<String, Object>>) attempt.get("questions");
    assertEquals(2, questions.size());
    assertFalse(questions.get(0).containsKey("correctOption"));

    List<Map<String, Object>> answers = new ArrayList<>();
    for (Map<String, Object> q : questions) {
      Map<String, Object> a = new HashMap<>();
      a.put("questionId", q.get("id"));
      a.put("selectedOption", "D");
      answers.add(a);
    }
    Map<String, Object> result =
        practice.submit(userId, ((Number) attempt.get("id")).longValue(), answers);
    int firstCorrect = ((Number) result.get("correctCount")).intValue();
    practice.submit(userId, ((Number) attempt.get("id")).longValue(), answers);
    assertEquals(
        firstCorrect,
        jdbc.queryForObject(
            "SELECT correct_count FROM practice_attempt WHERE id=?",
            Integer.class,
            attempt.get("id")));
    assertEquals(
        2 - firstCorrect,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM wrong_question WHERE user_id=?", Integer.class, userId));
    assertEquals(
        2,
        jdbc.queryForObject(
            "SELECT question_count FROM knowledge_chapter WHERE user_id=?", Integer.class, userId));
  }

  @Test
  void excelImportInsertsThenUpdatesByQuestionCode() throws Exception {
    byte[] first = workbook("EX-001", "原题干");
    Map<String, Object> inserted =
        bank.importExcel(
            new MockMultipartFile(
                "file",
                "questions.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                first));
    assertEquals(1, inserted.get("inserted"));
    Map<String, Object> updated =
        bank.importExcel(
            new MockMultipartFile(
                "file",
                "questions.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbook("EX-001", "更新题干")));
    assertEquals(1, updated.get("updated"));
    assertEquals(
        "更新题干",
        jdbc.queryForObject(
            "SELECT content FROM question_bank WHERE question_code='EX-001'", String.class));
    assertTrue(bank.template().length > 1000);
  }

  private long createUser(String name) {
    jdbc.update(
        "INSERT INTO sys_user(username,password_hash,nickname,role,enabled,token_version) VALUES(?,'x',?,'USER',1,0)",
        name,
        name);
    return jdbc.queryForObject("SELECT id FROM sys_user WHERE username=?", Long.class, name);
  }

  private Map<String, Object> question(String code, String answer, String content) {
    Map<String, Object> q = new HashMap<>();
    q.put("questionCode", code);
    q.put("chapter", "项目整合管理");
    q.put("content", content);
    q.put("optionA", "A项");
    q.put("optionB", "B项");
    q.put("optionC", "C项");
    q.put("optionD", "D项");
    q.put("correctOption", answer);
    q.put("explanation", "解析");
    q.put("enabled", true);
    return q;
  }

  private byte[] workbook(String code, String content) throws Exception {
    try (XSSFWorkbook wb = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      org.apache.poi.ss.usermodel.Sheet s = wb.createSheet("题库");
      org.apache.poi.ss.usermodel.Row h = s.createRow(0);
      for (int i = 0; i < QuestionBankService.HEADERS.length; i++)
        h.createCell(i).setCellValue(QuestionBankService.HEADERS[i]);
      org.apache.poi.ss.usermodel.Row r = s.createRow(1);
      String[] v = {code, "项目整合管理", content, "A项", "B项", "C项", "D项", "A", "解析", "是"};
      for (int i = 0; i < v.length; i++) r.createCell(i).setCellValue(v[i]);
      wb.write(out);
      return out.toByteArray();
    }
  }
}
