package com.sandy.ruankao.config;

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
@ConditionalOnProperty(name = "app.database-migration.enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseMigrationRunner implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        addColumnIfMissing("sys_user", "role",
                "ALTER TABLE sys_user ADD COLUMN role VARCHAR(10) NOT NULL DEFAULT 'USER'");
        addColumnIfMissing("sys_user", "enabled",
                "ALTER TABLE sys_user ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1");
        addColumnIfMissing("sys_user", "token_version",
                "ALTER TABLE sys_user ADD COLUMN token_version INT NOT NULL DEFAULT 0");
        addColumnIfMissing("study_plan", "user_id",
                "ALTER TABLE study_plan ADD COLUMN user_id BIGINT NULL AFTER id");
        addColumnIfMissing("knowledge_chapter", "user_id",
                "ALTER TABLE knowledge_chapter ADD COLUMN user_id BIGINT NULL AFTER id");
        addColumnIfMissing("wrong_question", "user_id",
                "ALTER TABLE wrong_question ADD COLUMN user_id BIGINT NULL AFTER id");

        addIndexIfMissing("study_plan", "idx_study_plan_user",
                "ALTER TABLE study_plan ADD INDEX idx_study_plan_user (user_id)");
        addIndexIfMissing("knowledge_chapter", "idx_chapter_user",
                "ALTER TABLE knowledge_chapter ADD INDEX idx_chapter_user (user_id)");
        addIndexIfMissing("wrong_question", "idx_wrong_question_user",
                "ALTER TABLE wrong_question ADD INDEX idx_wrong_question_user (user_id)");

        // 旧版本章节名全局唯一；多用户模式下改为同一用户内唯一。
        if (indexExists("knowledge_chapter", "uk_chapter_name")) {
            jdbcTemplate.execute("ALTER TABLE knowledge_chapter DROP INDEX uk_chapter_name");
        }
        addIndexIfMissing("knowledge_chapter", "uk_chapter_user_name",
                "ALTER TABLE knowledge_chapter ADD UNIQUE INDEX uk_chapter_user_name (user_id, name)");
    }

    private void addColumnIfMissing(String table, String column, String sql) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, table, column);
        if (count != null && count == 0) jdbcTemplate.execute(sql);
    }

    private void addIndexIfMissing(String table, String index, String sql) {
        if (!indexExists(table, index)) jdbcTemplate.execute(sql);
    }

    private boolean indexExists(String table, String index) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                Integer.class, table, index);
        return count != null && count > 0;
    }
}
