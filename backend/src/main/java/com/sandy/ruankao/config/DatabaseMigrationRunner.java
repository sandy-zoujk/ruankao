package com.sandy.ruankao.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 为旧版本数据库补齐用户权限和数据隔离字段。
 *
 * @author zoujunke
 */
@Component
@Order(0)
@ConditionalOnProperty(
    name = "app.database-migration.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class DatabaseMigrationRunner implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationRunner.class);
  private final JdbcTemplate jdbcTemplate;

  public DatabaseMigrationRunner(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void run(ApplicationArguments args) {
    long start = System.nanoTime();
    log.info("开始数据库结构迁移检查");
    // 本项目由业务层负责数据归属与删除规则，不在数据库层保留外键约束。
    dropAllForeignKeys();
    addColumnIfMissing(
        "sys_user",
        "role",
        "ALTER TABLE sys_user ADD COLUMN role VARCHAR(10) NOT NULL DEFAULT 'USER'");
    addColumnIfMissing(
        "sys_user",
        "enabled",
        "ALTER TABLE sys_user ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1");
    addColumnIfMissing(
        "sys_user",
        "token_version",
        "ALTER TABLE sys_user ADD COLUMN token_version INT NOT NULL DEFAULT 0");
    addColumnIfMissing(
        "study_plan", "user_id", "ALTER TABLE study_plan ADD COLUMN user_id BIGINT NULL AFTER id");
    addColumnIfMissing(
        "knowledge_chapter",
        "user_id",
        "ALTER TABLE knowledge_chapter ADD COLUMN user_id BIGINT NULL AFTER id");
    addColumnIfMissing(
        "wrong_question",
        "user_id",
        "ALTER TABLE wrong_question ADD COLUMN user_id BIGINT NULL AFTER id");
    addColumnIfMissing(
        "wrong_question",
        "source_question_id",
        "ALTER TABLE wrong_question ADD COLUMN source_question_id BIGINT NULL");

    addIndexIfMissing(
        "study_plan",
        "idx_study_plan_user",
        "ALTER TABLE study_plan ADD INDEX idx_study_plan_user (user_id)");
    addIndexIfMissing(
        "knowledge_chapter",
        "idx_chapter_user",
        "ALTER TABLE knowledge_chapter ADD INDEX idx_chapter_user (user_id)");
    addIndexIfMissing(
        "wrong_question",
        "idx_wrong_question_user",
        "ALTER TABLE wrong_question ADD INDEX idx_wrong_question_user (user_id)");
    addIndexIfMissing(
        "wrong_question",
        "uk_wrong_question_source",
        "ALTER TABLE wrong_question ADD UNIQUE INDEX uk_wrong_question_source (user_id, source_question_id)");

    // 旧版本章节名全局唯一；多用户模式下改为同一用户内唯一。
    if (indexExists("knowledge_chapter", "uk_chapter_name")) {
      jdbcTemplate.execute("ALTER TABLE knowledge_chapter DROP INDEX uk_chapter_name");
    }
    addIndexIfMissing(
        "knowledge_chapter",
        "uk_chapter_user_name",
        "ALTER TABLE knowledge_chapter ADD UNIQUE INDEX uk_chapter_user_name (user_id, name)");
    log.info("数据库结构迁移检查完成 durationMs={}", (System.nanoTime() - start) / 1_000_000);
  }

  private void dropAllForeignKeys() {
    List<Map<String, Object>> foreignKeys =
        jdbcTemplate.queryForList(
            "SELECT TABLE_NAME, CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND CONSTRAINT_TYPE = 'FOREIGN KEY'");
    log.debug("待移除外键 count={}", foreignKeys.size());
    for (Map<String, Object> foreignKey : foreignKeys) {
      String table = String.valueOf(foreignKey.get("TABLE_NAME"));
      String constraint = String.valueOf(foreignKey.get("CONSTRAINT_NAME"));
      // 名称来自 information_schema，并通过反引号转义后用于 DDL。
      jdbcTemplate.execute(
          "ALTER TABLE `"
              + table.replace("`", "``")
              + "` DROP FOREIGN KEY `"
              + constraint.replace("`", "``")
              + "`");
    }
  }

  private void addColumnIfMissing(String table, String column, String sql) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
            Integer.class,
            table,
            column);
    if (count != null && count == 0) jdbcTemplate.execute(sql);
  }

  private void addIndexIfMissing(String table, String index, String sql) {
    if (!indexExists(table, index)) jdbcTemplate.execute(sql);
  }

  private boolean indexExists(String table, String index) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.STATISTICS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
            Integer.class,
            table,
            index);
    return count != null && count > 0;
  }
}
