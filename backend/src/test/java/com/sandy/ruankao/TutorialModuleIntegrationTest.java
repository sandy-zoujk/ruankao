package com.sandy.ruankao;

import static org.junit.jupiter.api.Assertions.*;

import com.sandy.ruankao.service.TutorialFetchService;
import com.sandy.ruankao.service.TutorialService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class TutorialModuleIntegrationTest {
  @Autowired TutorialFetchService fetcher;
  @Autowired TutorialService tutorials;
  @Autowired JdbcTemplate jdbc;

  @Test
  void parserSanitizesDangerousHtmlAndSplitsSections() throws Exception {
    String html =
        "<html><head><title>测试教程</title></head><body><article><h1>教程标题</h1>"
            + "<p>这是一段足够长的导读文字，用来验证系统能够正确提取互联网公开教程的正文内容。</p>"
            + "<h2>第一节</h2><p onclick='bad()'>第一节包含足够长的学习内容，用于验证正文提取。</p><script>alert(1)</script>"
            + "<a href='javascript:alert(1)'>危险链接</a><img src='http://bad.test/x.png'>"
            + "<h2>第二节</h2><p>第二节也包含足够长的内容，确保能够生成独立的小节。</p></article></body></html>";
    TutorialFetchService.FetchResult result =
        fetcher.parseHtml(html, "https://example.com/tutorial");
    assertTrue(result.sections.size() >= 2);
    StringBuilder all = new StringBuilder();
    for (TutorialFetchService.SectionData section : result.sections) all.append(section.html);
    String content = all.toString();
    assertFalse(content.contains("script"));
    assertFalse(content.contains("onclick"));
    assertFalse(content.contains("javascript:"));
    assertFalse(content.contains("http://bad.test"));
  }

  @Test
  void urlValidationRejectsNonHttpsAndPrivateHosts() {
    assertThrows(
        IllegalArgumentException.class, () -> fetcher.validatePublicUrl("http://example.com/a"));
    assertThrows(
        IllegalArgumentException.class, () -> fetcher.validatePublicUrl("https://127.0.0.1/a"));
    assertThrows(
        IllegalArgumentException.class, () -> fetcher.validatePublicUrl("https://localhost/a"));
  }

  @Test
  void progressIsUserScopedAndRecalculatedForNewPublishedVersion() {
    long userA = user("tutorial_a"), userB = user("tutorial_b");
    jdbc.update(
        "INSERT INTO tutorial_source(source_name,source_url,chapter) VALUES('开放教程','https://example.com/tutorial','项目风险管理')");
    long source =
        jdbc.queryForObject(
            "SELECT id FROM tutorial_source WHERE source_url='https://example.com/tutorial'",
            Long.class);
    long v1 = version(source, "hash-one", "DRAFT");
    section(v1, "stable-a", "第一节", 1);
    section(v1, "stable-b", "第二节", 2);
    tutorials.publish(source, v1);
    tutorials.setCompleted(userA, source, "stable-a", true);
    assertEquals(50.0, ((Number) tutorials.progress(userA).get("progress")).doubleValue());
    assertEquals(0.0, ((Number) tutorials.progress(userB).get("progress")).doubleValue());
    long v2 = version(source, "hash-two", "DRAFT");
    section(v2, "stable-a", "第一节", 1);
    section(v2, "stable-b", "第二节", 2);
    section(v2, "new-c", "新增小节", 3);
    tutorials.publish(source, v2);
    assertEquals(33.33, ((Number) tutorials.progress(userA).get("progress")).doubleValue());
    assertEquals(3, ((List<?>) tutorials.detail(userA, source).get("sections")).size());
  }

  private long user(String name) {
    jdbc.update(
        "INSERT INTO sys_user(username,password_hash,nickname,role,enabled,token_version) VALUES(?,'x',?,'USER',1,0)",
        name,
        name);
    return jdbc.queryForObject("SELECT id FROM sys_user WHERE username=?", Long.class, name);
  }

  private long version(long source, String hash, String status) {
    jdbc.update(
        "INSERT INTO tutorial_version(source_id,title,summary,content_hash,status) VALUES(?,'教程','摘要',?,?)",
        source,
        hash,
        status);
    return jdbc.queryForObject(
        "SELECT id FROM tutorial_version WHERE source_id=? AND content_hash=?",
        Long.class,
        source,
        hash);
  }

  private void section(long version, String key, String title, int seq) {
    jdbc.update(
        "INSERT INTO tutorial_section(version_id,section_key,title,content_html,sequence_no) VALUES(?,?,?,'<p>安全正文内容足够长</p>',?)",
        version,
        key,
        title,
        seq);
  }
}
