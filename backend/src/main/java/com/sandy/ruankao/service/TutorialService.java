package com.sandy.ruankao.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.PreparedStatement;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/** @author zoujunke */
public class TutorialService {
  private static final Logger log = LoggerFactory.getLogger(TutorialService.class);
  private final JdbcTemplate jdbc;
  private final TutorialFetchService fetcher;

  public TutorialService(JdbcTemplate jdbc, TutorialFetchService fetcher) {
    this.jdbc = jdbc;
    this.fetcher = fetcher;
  }

  public List<Map<String, Object>> adminSources() {
    log.debug("查询教程来源");
    return jdbc.query(
        "SELECT s.*, (SELECT COUNT(*) FROM tutorial_version v WHERE v.source_id=s.id AND v.status='DRAFT') AS draft_count, (SELECT COUNT(*) FROM tutorial_version v WHERE v.source_id=s.id AND v.status='PUBLISHED') AS published_count FROM tutorial_source s ORDER BY s.updated_at DESC",
        (rs, n) -> {
          Map<String, Object> m = new LinkedHashMap<>();
          for (String c :
              new String[] {
                "id",
                "source_name",
                "source_url",
                "chapter",
                "enabled",
                "sync_status",
                "http_status",
                "last_error",
                "last_duration_ms",
                "last_synced_at",
                "last_success_at",
                "draft_count",
                "published_count"
              }) m.put(camel(c), rs.getObject(c));
          return m;
        });
  }

  public List<Map<String, Object>> versions(Long sourceId) {
    log.debug("查询教程版本");
    return jdbc.query(
        "SELECT id,title,summary,content_hash AS contentHash,status,fetched_at AS fetchedAt,published_at AS publishedAt,(SELECT COUNT(*) FROM tutorial_section s WHERE s.version_id=v.id) AS sectionCount FROM tutorial_version v WHERE source_id=? ORDER BY fetched_at DESC",
        (rs, n) -> {
          Map<String, Object> m = new LinkedHashMap<>();
          for (String c :
              new String[] {
                "id",
                "title",
                "summary",
                "contentHash",
                "status",
                "fetchedAt",
                "publishedAt",
                "sectionCount"
              }) m.put(c, rs.getObject(c));
          return m;
        },
        sourceId);
  }

  public Map<String, Object> saveSource(Long id, Map<String, Object> input) throws Exception {
    String name = req(input, "sourceName", "来源名称"),
        url = req(input, "sourceUrl", "来源网址"),
        chapter = req(input, "chapter", "归属章节");
    fetcher.validatePublicUrl(url);
    boolean enabled =
        input.get("enabled") == null || Boolean.parseBoolean(String.valueOf(input.get("enabled")));
    if (id == null)
      jdbc.update(
          "INSERT INTO tutorial_source(source_name,source_url,chapter,enabled) VALUES(?,?,?,?)",
          name,
          url,
          chapter,
          enabled);
    else if (jdbc.update(
            "UPDATE tutorial_source SET source_name=?,source_url=?,chapter=?,enabled=? WHERE id=?",
            name,
            url,
            chapter,
            enabled,
            id)
        == 0) throw new IllegalArgumentException("教程来源不存在");
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("sourceName", name);
    m.put("sourceUrl", url);
    m.put("chapter", chapter);
    m.put("enabled", enabled);
    log.info("教程来源保存处理完成 sourceId={} created={}", id, id == null);
    return m;
  }

  public Map<String, Object> sync(Long sourceId) {
    Map<String, Object> s = source(sourceId);
    long start = System.currentTimeMillis();
    log.info("开始同步教程 sourceId={}", sourceId);
    try {
      TutorialFetchService.FetchResult result = fetcher.fetch(String.valueOf(s.get("sourceUrl")));
      Integer existing =
          jdbc.queryForObject(
              "SELECT COUNT(*) FROM tutorial_version WHERE source_id=? AND content_hash=?",
              Integer.class,
              sourceId,
              result.hash);
      Long versionId = null;
      if (existing != null && existing > 0) {
        versionId =
            jdbc.queryForObject(
                "SELECT id FROM tutorial_version WHERE source_id=? AND content_hash=?",
                Long.class,
                sourceId,
                result.hash);
      } else {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(
            c -> {
              PreparedStatement ps =
                  c.prepareStatement(
                      "INSERT INTO tutorial_version(source_id,title,summary,content_hash,status) VALUES(?,?,?,?,'DRAFT')",
                      new String[] {"id"});
              ps.setLong(1, sourceId);
              ps.setString(2, result.title);
              ps.setString(3, result.summary);
              ps.setString(4, result.hash);
              return ps;
            },
            kh);
        versionId = Objects.requireNonNull(kh.getKey()).longValue();
        for (TutorialFetchService.SectionData section : result.sections)
          jdbc.update(
              "INSERT INTO tutorial_section(version_id,section_key,title,content_html,sequence_no) VALUES(?,?,?,?,?)",
              versionId,
              section.key,
              section.title,
              section.html,
              section.sequence);
      }
      jdbc.update(
          "UPDATE tutorial_source SET sync_status='SUCCESS',http_status=?,last_error=NULL,last_duration_ms=?,last_synced_at=CURRENT_TIMESTAMP,last_success_at=CURRENT_TIMESTAMP WHERE id=?",
          result.httpStatus,
          result.durationMs,
          sourceId);
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("versionId", versionId);
      m.put("changed", existing == null || existing == 0);
      m.put("title", result.title);
      m.put("sectionCount", result.sections.size());
      log.info("教程同步处理完成 sourceId={} versionId={} sectionCount={} changed={} durationMs={}",
          sourceId, versionId, result.sections.size(), existing == null || existing == 0,
          System.currentTimeMillis() - start);
      return m;
    } catch (Exception e) {
      log.error("教程同步失败 sourceId={}", sourceId,
          com.sandy.ruankao.logging.LogExceptions.withoutMessages(e));
      jdbc.update(
          "UPDATE tutorial_source SET sync_status='FAILED',last_error=?,last_duration_ms=?,last_synced_at=CURRENT_TIMESTAMP WHERE id=?",
          truncate(e.getMessage(), 1000),
          System.currentTimeMillis() - start,
          sourceId);
      throw new IllegalArgumentException("同步失败：" + e.getMessage());
    }
  }

  @Transactional
  public void publish(Long sourceId, Long versionId) {
    Integer count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM tutorial_version WHERE id=? AND source_id=? AND status IN ('DRAFT','PUBLISHED')",
            Integer.class,
            versionId,
            sourceId);
    if (count == null || count == 0) throw new IllegalArgumentException("教程草稿不存在");
    jdbc.update(
        "UPDATE tutorial_version SET status='ARCHIVED' WHERE source_id=? AND status='PUBLISHED' AND id<>?",
        sourceId,
        versionId);
    jdbc.update(
        "UPDATE tutorial_version SET status='PUBLISHED',published_at=CURRENT_TIMESTAMP WHERE id=? AND source_id=?",
        versionId,
        sourceId);
    log.info("教程发布处理完成 sourceId={} versionId={}", sourceId, versionId);
  }

  public void unpublish(Long sourceId) {
    jdbc.update(
        "UPDATE tutorial_version SET status='ARCHIVED' WHERE source_id=? AND status='PUBLISHED'",
        sourceId);
    log.info("教程下架处理完成 sourceId={}", sourceId);
  }

  public Map<String, Object> preview(Long sourceId, Long versionId) {
    log.debug("预览教程");
    Map<String, Object> s = source(sourceId);
    Map<String, Object> v = version(versionId, sourceId, false);
    v.put("source", s);
    v.put("sections", sections(versionId, null, sourceId));
    return v;
  }

  public List<Map<String, Object>> catalog(Long userId, String chapter) {
    log.debug("查询教程目录");
    String condition = chapter == null || chapter.trim().isEmpty() ? "" : " AND s.chapter=?";
    Object[] args =
        condition.isEmpty() ? new Object[] {userId} : new Object[] {userId, chapter.trim()};
    return jdbc.query(
        "SELECT s.id AS sourceId,s.source_name AS sourceName,s.source_url AS sourceUrl,s.chapter,v.id AS versionId,v.title,v.summary,v.published_at AS publishedAt,COUNT(sec.id) AS sectionCount,COUNT(p.id) AS completedCount FROM tutorial_source s JOIN tutorial_version v ON v.source_id=s.id AND v.status='PUBLISHED' JOIN tutorial_section sec ON sec.version_id=v.id LEFT JOIN user_tutorial_progress p ON p.user_id=? AND p.source_id=s.id AND p.section_key=sec.section_key AND p.completed=1 WHERE s.enabled=1"
            + condition
            + " GROUP BY s.id,s.source_name,s.source_url,s.chapter,v.id,v.title,v.summary,v.published_at ORDER BY s.chapter,v.published_at DESC",
        args,
        (rs, n) -> {
          Map<String, Object> m = new LinkedHashMap<>();
          for (String c :
              new String[] {
                "sourceId",
                "sourceName",
                "sourceUrl",
                "chapter",
                "versionId",
                "title",
                "summary",
                "publishedAt",
                "sectionCount",
                "completedCount"
              }) m.put(c, rs.getObject(c));
          long total = rs.getLong("sectionCount"), done = rs.getLong("completedCount");
          m.put("progress", total == 0 ? 0 : Math.round(done * 10000.0 / total) / 100.0);
          return m;
        });
  }

  public Map<String, Object> detail(Long userId, Long sourceId) {
    log.debug("查询教程详情");
    Map<String, Object> s = source(sourceId);
    Object enabled = s.get("enabled");
    if ((enabled instanceof Boolean && !((Boolean) enabled))
        || (enabled instanceof Number && ((Number) enabled).intValue() == 0))
      throw new IllegalArgumentException("教程已下架");
    Map<String, Object> v = version(null, sourceId, true);
    v.put("source", s);
    v.put("sections", sections(((Number) v.get("id")).longValue(), userId, sourceId));
    return v;
  }

  public Map<String, Object> progress(Long userId) {
    log.debug("查询教程进度");
    List<Map<String, Object>> items = catalog(userId, null);
    long total = 0, done = 0;
    Map<String, long[]> chapters = new LinkedHashMap<>();
    for (Map<String, Object> i : items) {
      long t = ((Number) i.get("sectionCount")).longValue(),
          d = ((Number) i.get("completedCount")).longValue();
      total += t;
      done += d;
      long[] x = chapters.computeIfAbsent(String.valueOf(i.get("chapter")), k -> new long[2]);
      x[0] += t;
      x[1] += d;
    }
    List<Map<String, Object>> chapterList = new ArrayList<>();
    for (Map.Entry<String, long[]> e : chapters.entrySet()) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("chapter", e.getKey());
      m.put("sectionCount", e.getValue()[0]);
      m.put("completedCount", e.getValue()[1]);
      m.put(
          "progress",
          e.getValue()[0] == 0
              ? 0
              : Math.round(e.getValue()[1] * 10000.0 / e.getValue()[0]) / 100.0);
      chapterList.add(m);
    }
    Map<String, Object> r = new LinkedHashMap<>();
    r.put("sectionCount", total);
    r.put("completedCount", done);
    r.put("progress", total == 0 ? 0 : Math.round(done * 10000.0 / total) / 100.0);
    r.put("chapters", chapterList);
    return r;
  }

  @Transactional
  public void setCompleted(Long userId, Long sourceId, String sectionKey, boolean completed) {
    detail(userId, sourceId);
    Integer valid =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM tutorial_section sec JOIN tutorial_version v ON v.id=sec.version_id WHERE v.source_id=? AND v.status='PUBLISHED' AND sec.section_key=?",
            Integer.class,
            sourceId,
            sectionKey);
    if (valid == null || valid == 0) throw new IllegalArgumentException("教程小节不存在");
    Integer count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM user_tutorial_progress WHERE user_id=? AND source_id=? AND section_key=?",
            Integer.class,
            userId,
            sourceId,
            sectionKey);
    if (count != null && count > 0)
      jdbc.update(
          "UPDATE user_tutorial_progress SET completed=?,completed_at=CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE completed_at END WHERE user_id=? AND source_id=? AND section_key=?",
          completed,
          completed,
          userId,
          sourceId,
          sectionKey);
    else
      jdbc.update(
          "INSERT INTO user_tutorial_progress(user_id,source_id,section_key,completed) VALUES(?,?,?,?)",
          userId,
          sourceId,
          sectionKey,
          completed);
    log.info("教程学习状态更新处理完成 userId={} sourceId={} completed={}", userId, sourceId, completed);
  }

  private Map<String, Object> source(Long id) {
    List<Map<String, Object>> l =
        jdbc.query(
            "SELECT id,source_name AS sourceName,source_url AS sourceUrl,chapter,enabled,sync_status AS syncStatus,last_error AS lastError,last_synced_at AS lastSyncedAt,last_success_at AS lastSuccessAt FROM tutorial_source WHERE id=?",
            (rs, n) -> {
              Map<String, Object> m = new LinkedHashMap<>();
              for (String c :
                  new String[] {
                    "id",
                    "sourceName",
                    "sourceUrl",
                    "chapter",
                    "enabled",
                    "syncStatus",
                    "lastError",
                    "lastSyncedAt",
                    "lastSuccessAt"
                  }) m.put(c, rs.getObject(c));
              return m;
            },
            id);
    if (l.isEmpty()) throw new IllegalArgumentException("教程来源不存在");
    return l.get(0);
  }

  private Map<String, Object> version(Long versionId, Long sourceId, boolean published) {
    String sql =
        "SELECT id,title,summary,status,fetched_at AS fetchedAt,published_at AS publishedAt FROM tutorial_version WHERE source_id=?"
            + (versionId == null ? " AND status='PUBLISHED'" : " AND id=?")
            + " ORDER BY published_at DESC LIMIT 1";
    Object[] args =
        versionId == null ? new Object[] {sourceId} : new Object[] {sourceId, versionId};
    List<Map<String, Object>> l =
        jdbc.query(
            sql,
            args,
            (rs, n) -> {
              Map<String, Object> m = new LinkedHashMap<>();
              for (String c :
                  new String[] {"id", "title", "summary", "status", "fetchedAt", "publishedAt"})
                m.put(c, rs.getObject(c));
              return m;
            });
    if (l.isEmpty()) throw new IllegalArgumentException(published ? "教程尚未发布" : "教程版本不存在");
    return l.get(0);
  }

  private List<Map<String, Object>> sections(Long versionId, Long userId, Long sourceId) {
    if (userId == null)
      return jdbc.query(
          "SELECT section_key AS sectionKey,title,content_html AS contentHtml,sequence_no AS sequenceNo FROM tutorial_section WHERE version_id=? ORDER BY sequence_no",
          (rs, n) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            for (String c : new String[] {"sectionKey", "title", "contentHtml", "sequenceNo"})
              m.put(c, rs.getObject(c));
            return m;
          },
          versionId);
    return jdbc.query(
        "SELECT sec.section_key AS sectionKey,sec.title,sec.content_html AS contentHtml,sec.sequence_no AS sequenceNo,CASE WHEN p.completed=1 THEN 1 ELSE 0 END AS completed FROM tutorial_section sec LEFT JOIN user_tutorial_progress p ON p.user_id=? AND p.source_id=? AND p.section_key=sec.section_key WHERE sec.version_id=? ORDER BY sec.sequence_no",
        (rs, n) -> {
          Map<String, Object> m = new LinkedHashMap<>();
          for (String c :
              new String[] {"sectionKey", "title", "contentHtml", "sequenceNo", "completed"})
            m.put(c, rs.getObject(c));
          return m;
        },
        userId,
        sourceId,
        versionId);
  }

  private static String req(Map<String, Object> m, String k, String label) {
    String v = Objects.toString(m.get(k), "").trim();
    if (v.isEmpty()) throw new IllegalArgumentException(label + "不能为空");
    return v;
  }

  private static String truncate(String s, int max) {
    if (s == null) return "未知错误";
    return s.length() > max ? s.substring(0, max) : s;
  }

  private static String camel(String s) {
    StringBuilder b = new StringBuilder();
    boolean upper = false;
    for (char c : s.toCharArray()) {
      if (c == '_') {
        upper = true;
      } else {
        b.append(upper ? Character.toUpperCase(c) : c);
        upper = false;
      }
    }
    return b.toString();
  }
}
