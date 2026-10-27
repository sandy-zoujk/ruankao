package com.sandy.ruankao.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/** @author zoujunke */
public class PracticeService {
  private static final Logger log = LoggerFactory.getLogger(PracticeService.class);
  private final JdbcTemplate jdbc;

  public PracticeService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> chapters() {
    log.debug("查询练习章节");
    return jdbc.query(
        "SELECT chapter,COUNT(*) AS questionCount FROM question_bank WHERE enabled=1 GROUP BY chapter ORDER BY chapter",
        (rs, n) -> {
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("chapter", rs.getString("chapter"));
          m.put("questionCount", rs.getInt("questionCount"));
          return m;
        });
  }

  @Transactional
  public Map<String, Object> create(Long userId, String chapter, int count) {
    if (count < 1 || count > 100) throw new IllegalArgumentException("题量必须在 1 到 100 之间");
    String condition = chapter == null || chapter.trim().isEmpty() ? "" : " AND chapter=?";
    Object[] args =
        condition.isEmpty() ? new Object[] {count} : new Object[] {chapter.trim(), count};
    List<Long> ids =
        jdbc.query(
            "SELECT id FROM question_bank WHERE enabled=1" + condition + " ORDER BY RAND() LIMIT ?",
            args,
            (rs, n) -> rs.getLong(1));
    if (ids.size() < count) throw new IllegalArgumentException("当前条件仅有 " + ids.size() + " 道可用题目");
    KeyHolder kh = new GeneratedKeyHolder();
    String finalChapter = chapter == null || chapter.trim().isEmpty() ? null : chapter.trim();
    jdbc.update(
        c -> {
          PreparedStatement ps =
              c.prepareStatement(
                  "INSERT INTO practice_attempt(user_id,chapter,question_count,submitted) VALUES(?,?,?,0)",
                  new String[] {"id"});
          ps.setLong(1, userId);
          ps.setString(2, finalChapter);
          ps.setInt(3, count);
          return ps;
        },
        kh);
    long attemptId = Objects.requireNonNull(kh.getKey()).longValue();
    for (int i = 0; i < ids.size(); i++)
      jdbc.update(
          "INSERT INTO practice_answer(attempt_id,question_id,sequence_no) VALUES(?,?,?)",
          attemptId,
          ids.get(i),
          i + 1);
    Map<String, Object> result = detail(userId, attemptId, false);
    log.info("练习创建处理完成 userId={} attemptId={} questionCount={}", userId, attemptId, count);
    return result;
  }

  @Transactional
  public Map<String, Object> submit(
      Long userId, Long attemptId, List<Map<String, Object>> submittedAnswers) {
    Map<String, Object> attempt = findAttemptForUpdate(userId, attemptId);
    if (Boolean.TRUE.equals(asBoolean(attempt.get("submitted"))))
      return detail(userId, attemptId, true);
    Map<Long, String> choices = new HashMap<>();
    if (submittedAnswers != null)
      for (Map<String, Object> a : submittedAnswers) {
        Long q = Long.valueOf(String.valueOf(a.get("questionId")));
        String option = Objects.toString(a.get("selectedOption"), "").trim().toUpperCase();
        if (!option.isEmpty() && !option.matches("[A-D]"))
          throw new IllegalArgumentException("答案只能是 A、B、C 或 D");
        choices.put(q, option.isEmpty() ? null : option);
      }
    List<Map<String, Object>> questions = answerRows(attemptId);
    int correct = 0;
    for (Map<String, Object> q : questions) {
      long qid = ((Number) q.get("id")).longValue();
      String selected = choices.get(qid);
      boolean ok = Objects.equals(q.get("correctOption"), selected);
      if (ok) correct++;
      jdbc.update(
          "UPDATE practice_answer SET selected_option=?,correct=? WHERE attempt_id=? AND question_id=?",
          selected,
          ok,
          attemptId,
          qid);
      updateChapter(userId, String.valueOf(q.get("chapter")), ok);
      if (!ok) syncWrongQuestion(userId, q, selected);
    }
    double score =
        questions.isEmpty() ? 0 : Math.round(correct * 10000.0 / questions.size()) / 100.0;
    jdbc.update(
        "UPDATE practice_attempt SET correct_count=?,score=?,submitted=1,submitted_at=CURRENT_TIMESTAMP WHERE id=? AND user_id=? AND submitted=0",
        correct,
        score,
        attemptId,
        userId);
    Map<String, Object> result = detail(userId, attemptId, true);
    log.info("练习提交处理完成 userId={} attemptId={} questionCount={}", userId, attemptId, questions.size());
    return result;
  }

  public Map<String, Object> detail(Long userId, Long attemptId, boolean includeAnswers) {
    Map<String, Object> attempt = findAttempt(userId, attemptId);
    boolean submitted = Boolean.TRUE.equals(asBoolean(attempt.get("submitted")));
    List<Map<String, Object>> rows = answerRows(attemptId);
    List<Map<String, Object>> questions = new ArrayList<>();
    for (Map<String, Object> row : rows) {
      Map<String, Object> q = new LinkedHashMap<>();
      for (String k :
          new String[] {
            "id",
            "questionCode",
            "chapter",
            "content",
            "optionA",
            "optionB",
            "optionC",
            "optionD",
            "sequenceNo"
          }) q.put(k, row.get(k));
      if (submitted && includeAnswers) {
        q.put("selectedOption", row.get("selectedOption"));
        q.put("correctOption", row.get("correctOption"));
        q.put("correct", row.get("correct"));
        q.put("explanation", row.get("explanation"));
      }
      questions.add(q);
    }
    attempt.put("questions", questions);
    log.debug("练习详情查询完成 userId={} attemptId={} questionCount={}", userId, attemptId, questions.size());
    return attempt;
  }

  public List<Map<String, Object>> history(Long userId) {
    log.debug("查询练习历史 userId={}", userId);
    return jdbc.query(
        "SELECT id,chapter,question_count AS questionCount,correct_count AS correctCount,score,started_at AS startedAt,submitted_at AS submittedAt FROM practice_attempt WHERE user_id=? AND submitted=1 ORDER BY submitted_at DESC LIMIT 50",
        (rs, n) -> {
          Map<String, Object> m = new LinkedHashMap<>();
          for (String c :
              new String[] {
                "id",
                "chapter",
                "questionCount",
                "correctCount",
                "score",
                "startedAt",
                "submittedAt"
              }) m.put(c, rs.getObject(c));
          return m;
        },
        userId);
  }

  public Map<String, Object> stats(Long userId) {
    log.debug("查询练习统计 userId={}", userId);
    return jdbc.queryForObject(
        "SELECT COUNT(*) AS attemptCount,COALESCE(SUM(question_count),0) AS questionCount,COALESCE(SUM(correct_count),0) AS correctCount FROM practice_attempt WHERE user_id=? AND submitted=1",
        (rs, n) -> {
          long total = rs.getLong("questionCount"), correct = rs.getLong("correctCount");
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("attemptCount", rs.getLong("attemptCount"));
          m.put("questionCount", total);
          m.put("correctCount", correct);
          m.put("accuracy", total == 0 ? 0 : Math.round(correct * 10000.0 / total) / 100.0);
          return m;
        },
        userId);
  }

  private Map<String, Object> findAttempt(Long userId, Long id) {
    List<Map<String, Object>> list =
        jdbc.query(
            "SELECT id,chapter,question_count AS questionCount,correct_count AS correctCount,score,submitted,started_at AS startedAt,submitted_at AS submittedAt FROM practice_attempt WHERE id=? AND user_id=?",
            (rs, n) -> {
              Map<String, Object> m = new LinkedHashMap<>();
              for (String c :
                  new String[] {
                    "id",
                    "chapter",
                    "questionCount",
                    "correctCount",
                    "score",
                    "submitted",
                    "startedAt",
                    "submittedAt"
                  }) m.put(c, rs.getObject(c));
              return m;
            },
            id,
            userId);
    if (list.isEmpty()) throw new IllegalArgumentException("练习记录不存在");
    return list.get(0);
  }

  private Map<String, Object> findAttemptForUpdate(Long userId, Long id) {
    List<Map<String, Object>> list =
        jdbc.query(
            "SELECT id,chapter,question_count AS questionCount,correct_count AS correctCount,score,submitted,started_at AS startedAt,submitted_at AS submittedAt FROM practice_attempt WHERE id=? AND user_id=? FOR UPDATE",
            (rs, n) -> {
              Map<String, Object> m = new LinkedHashMap<>();
              for (String c :
                  new String[] {
                    "id",
                    "chapter",
                    "questionCount",
                    "correctCount",
                    "score",
                    "submitted",
                    "startedAt",
                    "submittedAt"
                  }) m.put(c, rs.getObject(c));
              return m;
            },
            id,
            userId);
    if (list.isEmpty()) throw new IllegalArgumentException("练习记录不存在");
    return list.get(0);
  }

  private List<Map<String, Object>> answerRows(Long attemptId) {
    return jdbc.query(
        "SELECT q.id,q.question_code AS questionCode,q.chapter,q.content,q.option_a AS optionA,q.option_b AS optionB,q.option_c AS optionC,q.option_d AS optionD,q.correct_option AS correctOption,q.explanation,a.sequence_no AS sequenceNo,a.selected_option AS selectedOption,a.correct FROM practice_answer a JOIN question_bank q ON q.id=a.question_id WHERE a.attempt_id=? ORDER BY a.sequence_no",
        (rs, n) -> {
          Map<String, Object> m = new LinkedHashMap<>();
          for (String c :
              new String[] {
                "id",
                "questionCode",
                "chapter",
                "content",
                "optionA",
                "optionB",
                "optionC",
                "optionD",
                "correctOption",
                "explanation",
                "sequenceNo",
                "selectedOption",
                "correct"
              }) m.put(c, rs.getObject(c));
          return m;
        },
        attemptId);
  }

  private void updateChapter(Long userId, String chapter, boolean correct) {
    jdbc.update(
        "UPDATE knowledge_chapter SET question_count=question_count+1,correct_count=correct_count+?,last_review_date=? WHERE user_id=? AND name=?",
        correct ? 1 : 0,
        LocalDate.now(),
        userId,
        chapter);
  }

  private void syncWrongQuestion(Long userId, Map<String, Object> q, String selected) {
    long qid = ((Number) q.get("id")).longValue();
    Integer count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM wrong_question WHERE user_id=? AND source_question_id=?",
            Integer.class,
            userId,
            qid);
    String content =
        q.get("content")
            + "\nA. "
            + q.get("optionA")
            + "\nB. "
            + q.get("optionB")
            + "\nC. "
            + q.get("optionC")
            + "\nD. "
            + q.get("optionD");
    String correct =
        "正确答案："
            + q.get("correctOption")
            + (Objects.toString(q.get("explanation"), "").isEmpty()
                ? ""
                : "\n解析：" + q.get("explanation"));
    if (count != null && count > 0)
      jdbc.update(
          "UPDATE wrong_question SET content=?,correct_answer=?,my_answer=?,error_reason='练习答错',mastered=0,next_review_date=? WHERE user_id=? AND source_question_id=?",
          content,
          correct,
          selected,
          LocalDate.now().plusDays(1),
          userId,
          qid);
    else
      jdbc.update(
          "INSERT INTO wrong_question(user_id,title,content,correct_answer,my_answer,error_reason,review_count,mastered,next_review_date,source_question_id) VALUES(?,?,?,?,?,'练习答错',0,0,?,?)",
          userId,
          "选择题：" + q.get("questionCode"),
          content,
          correct,
          selected,
          LocalDate.now().plusDays(1),
          qid);
  }

  private Boolean asBoolean(Object value) {
    if (value instanceof Boolean) return (Boolean) value;
    if (value instanceof Number) return ((Number) value).intValue() != 0;
    return Boolean.valueOf(String.valueOf(value));
  }
}
