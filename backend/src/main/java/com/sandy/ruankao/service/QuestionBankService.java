package com.sandy.ruankao.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayOutputStream;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
/** @author zoujunke */
public class QuestionBankService {
  private static final Logger log = LoggerFactory.getLogger(QuestionBankService.class);
  public static final String[] HEADERS = {
    "题目编号", "章节", "题干", "选项A", "选项B", "选项C", "选项D", "正确答案", "解析", "是否启用"
  };
  private final JdbcTemplate jdbc;
  private final NamedParameterJdbcTemplate namedJdbc;

  public QuestionBankService(JdbcTemplate jdbc, NamedParameterJdbcTemplate namedJdbc) {
    this.jdbc = jdbc;
    this.namedJdbc = namedJdbc;
  }

  public Map<String, Object> list(
      String keyword, String chapter, Boolean enabled, int page, int size) {
    page = Math.max(1, page);
    size = Math.max(1, Math.min(100, size));
    StringBuilder where = new StringBuilder(" WHERE 1=1");
    MapSqlParameterSource params = new MapSqlParameterSource();
    if (keyword != null && !keyword.trim().isEmpty()) {
      where.append(" AND (question_code LIKE :keyword OR content LIKE :keyword)");
      params.addValue("keyword", "%" + keyword.trim() + "%");
    }
    if (chapter != null && !chapter.trim().isEmpty()) {
      where.append(" AND chapter=:chapter");
      params.addValue("chapter", chapter.trim());
    }
    if (enabled != null) {
      where.append(" AND enabled=:enabled");
      params.addValue("enabled", enabled);
    }
    Long total =
        namedJdbc.queryForObject("SELECT COUNT(*) FROM question_bank" + where, params, Long.class);
    params.addValue("limit", size).addValue("offset", (page - 1) * size);
    List<Map<String, Object>> items =
        namedJdbc.query(
            "SELECT id,question_code AS questionCode,chapter,content,option_a AS optionA,option_b AS optionB,"
                + "option_c AS optionC,option_d AS optionD,correct_option AS correctOption,explanation,enabled,created_at AS createdAt,updated_at AS updatedAt "
                + "FROM question_bank"
                + where
                + " ORDER BY updated_at DESC,id DESC LIMIT :limit OFFSET :offset",
            params,
            (rs, n) -> row(rs));
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("items", items);
    result.put("total", total);
    result.put("page", page);
    result.put("size", size);
    log.debug("题库查询完成 page={} size={} count={}", page, size, items.size());
    return result;
  }

  @Transactional
  public Map<String, Object> save(Long id, Map<String, Object> input) {
    Map<String, Object> q = normalize(input);
    if (id == null) {
      jdbc.update(
          "INSERT INTO question_bank(question_code,chapter,content,option_a,option_b,option_c,option_d,correct_option,explanation,enabled) VALUES(?,?,?,?,?,?,?,?,?,?)",
          q.get("questionCode"),
          q.get("chapter"),
          q.get("content"),
          q.get("optionA"),
          q.get("optionB"),
          q.get("optionC"),
          q.get("optionD"),
          q.get("correctOption"),
          q.get("explanation"),
          q.get("enabled"));
    } else {
      int changed =
          jdbc.update(
              "UPDATE question_bank SET question_code=?,chapter=?,content=?,option_a=?,option_b=?,option_c=?,option_d=?,correct_option=?,explanation=?,enabled=? WHERE id=?",
              q.get("questionCode"),
              q.get("chapter"),
              q.get("content"),
              q.get("optionA"),
              q.get("optionB"),
              q.get("optionC"),
              q.get("optionD"),
              q.get("correctOption"),
              q.get("explanation"),
              q.get("enabled"),
              id);
      if (changed == 0) throw new IllegalArgumentException("题目不存在");
    }
    log.info("题目保存处理完成 questionId={} created={}", id, id == null);
    return q;
  }

  public void setEnabled(Long id, boolean enabled) {
    if (jdbc.update("UPDATE question_bank SET enabled=? WHERE id=?", enabled, id) == 0)
      throw new IllegalArgumentException("题目不存在");
    log.info("题目启用状态调整处理完成 questionId={} enabled={}", id, enabled);
  }

  @Transactional
  public void delete(Long id) {
    Integer used =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM practice_answer WHERE question_id=?", Integer.class, id);
    if (used != null && used > 0) {
      setEnabled(id, false);
      return;
    }
    if (jdbc.update("DELETE FROM question_bank WHERE id=?", id) == 0)
      throw new IllegalArgumentException("题目不存在");
    log.info("题目删除处理完成 questionId={}", id);
  }

  public byte[] template() throws Exception {
    try (Workbook wb = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet("题库");
      Row header = sheet.createRow(0);
      CellStyle style = wb.createCellStyle();
      Font font = wb.createFont();
      font.setBold(true);
      style.setFont(font);
      style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
      style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      for (int i = 0; i < HEADERS.length; i++) {
        Cell cell = header.createCell(i);
        cell.setCellValue(HEADERS[i]);
        cell.setCellStyle(style);
        sheet.setColumnWidth(i, i == 2 || i == 8 ? 12000 : 5000);
      }
      Row sample = sheet.createRow(1);
      String[] values = {
        "PM-001", "项目整合管理", "示例题干（导入前请删除示例行）", "选项A", "选项B", "选项C", "选项D", "A", "答案解析", "是"
      };
      for (int i = 0; i < values.length; i++) sample.createCell(i).setCellValue(values[i]);
      sheet.createFreezePane(0, 1);
      wb.write(out);
      return out.toByteArray();
    }
  }

  @Transactional
  public Map<String, Object> importExcel(MultipartFile file) throws Exception {
    if (file == null
        || file.isEmpty()
        || file.getOriginalFilename() == null
        || !file.getOriginalFilename().toLowerCase().endsWith(".xlsx"))
      throw new IllegalArgumentException("请上传 .xlsx 格式文件");
    log.info("开始导入题库 sizeBytes={}", file.getSize());
    int inserted = 0, updated = 0;
    List<String> errors = new ArrayList<>();
    try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
      Sheet sheet = wb.getSheetAt(0);
      DataFormatter fmt = new DataFormatter();
      Row header = sheet.getRow(0);
      for (int i = 0; i < HEADERS.length; i++)
        if (header == null || !HEADERS[i].equals(fmt.formatCellValue(header.getCell(i)).trim()))
          throw new IllegalArgumentException(
              "Excel 表头不符合模板，第 " + (i + 1) + " 列应为“" + HEADERS[i] + "”");
      for (int r = 1; r <= sheet.getLastRowNum(); r++) {
        Row row = sheet.getRow(r);
        if (row == null) continue;
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("questionCode", text(fmt, row, 0));
        q.put("chapter", text(fmt, row, 1));
        q.put("content", text(fmt, row, 2));
        q.put("optionA", text(fmt, row, 3));
        q.put("optionB", text(fmt, row, 4));
        q.put("optionC", text(fmt, row, 5));
        q.put("optionD", text(fmt, row, 6));
        q.put("correctOption", text(fmt, row, 7));
        q.put("explanation", text(fmt, row, 8));
        q.put("enabled", !"否".equals(text(fmt, row, 9)));
        if (String.valueOf(q.get("questionCode")).trim().isEmpty()
            && String.valueOf(q.get("content")).trim().isEmpty()) continue;
        try {
          q = normalize(q);
          Integer count =
              jdbc.queryForObject(
                  "SELECT COUNT(*) FROM question_bank WHERE question_code=?",
                  Integer.class,
                  q.get("questionCode"));
          if (count != null && count > 0) {
            jdbc.update(
                "UPDATE question_bank SET chapter=?,content=?,option_a=?,option_b=?,option_c=?,option_d=?,correct_option=?,explanation=?,enabled=? WHERE question_code=?",
                q.get("chapter"),
                q.get("content"),
                q.get("optionA"),
                q.get("optionB"),
                q.get("optionC"),
                q.get("optionD"),
                q.get("correctOption"),
                q.get("explanation"),
                q.get("enabled"),
                q.get("questionCode"));
            updated++;
          } else {
            save(null, q);
            inserted++;
          }
        } catch (Exception e) {
          errors.add("第 " + (r + 1) + " 行：" + e.getMessage());
        }
      }
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("inserted", inserted);
    result.put("updated", updated);
    result.put("failed", errors.size());
    result.put("errors", errors);
    log.info("题库导入处理完成 inserted={} updated={} failed={}", inserted, updated, errors.size());
    return result;
  }

  private static String text(DataFormatter fmt, Row row, int col) {
    return fmt.formatCellValue(row.getCell(col)).trim();
  }

  private static String required(Map<String, Object> q, String key, String label) {
    String value = Objects.toString(q.get(key), "").trim();
    if (value.isEmpty()) throw new IllegalArgumentException(label + "不能为空");
    return value;
  }

  private Map<String, Object> normalize(Map<String, Object> input) {
    Map<String, Object> q = new LinkedHashMap<>();
    q.put("questionCode", required(input, "questionCode", "题目编号"));
    q.put("chapter", required(input, "chapter", "章节"));
    q.put("content", required(input, "content", "题干"));
    q.put("optionA", required(input, "optionA", "选项A"));
    q.put("optionB", required(input, "optionB", "选项B"));
    q.put("optionC", required(input, "optionC", "选项C"));
    q.put("optionD", required(input, "optionD", "选项D"));
    String answer = required(input, "correctOption", "正确答案").toUpperCase();
    if (!answer.matches("[A-D]")) throw new IllegalArgumentException("正确答案只能是 A、B、C 或 D");
    q.put("correctOption", answer);
    q.put("explanation", Objects.toString(input.get("explanation"), "").trim());
    q.put(
        "enabled",
        input.get("enabled") == null || Boolean.parseBoolean(String.valueOf(input.get("enabled"))));
    return q;
  }

  private Map<String, Object> row(java.sql.ResultSet rs) throws java.sql.SQLException {
    Map<String, Object> m = new LinkedHashMap<>();
    String[] cols = {
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
      "enabled",
      "createdAt",
      "updatedAt"
    };
    for (String c : cols) m.put(c, rs.getObject(c));
    return m;
  }
}
