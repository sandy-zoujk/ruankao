CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(30) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  nickname VARCHAR(50) NOT NULL,
  role VARCHAR(10) NOT NULL DEFAULT 'USER',
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  token_version INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_sys_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS study_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  title VARCHAR(120) NOT NULL,
  category VARCHAR(50) NOT NULL,
  plan_date DATE NOT NULL,
  duration_minutes INT NOT NULL DEFAULT 60,
  completed TINYINT(1) NOT NULL DEFAULT 0,
  notes VARCHAR(500),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_study_plan_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS knowledge_chapter (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  name VARCHAR(100) NOT NULL,
  category VARCHAR(50) NOT NULL,
  priority INT NOT NULL DEFAULT 3,
  mastery INT NOT NULL DEFAULT 0,
  question_count INT NOT NULL DEFAULT 0,
  correct_count INT NOT NULL DEFAULT 0,
  last_review_date DATE,
  UNIQUE KEY uk_chapter_user_name (user_id, name),
  KEY idx_chapter_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS wrong_question (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  title VARCHAR(200) NOT NULL,
  content TEXT NOT NULL,
  correct_answer TEXT,
  my_answer TEXT,
  error_reason VARCHAR(50) NOT NULL,
  chapter_id BIGINT,
  review_count INT NOT NULL DEFAULT 0,
  mastered TINYINT(1) NOT NULL DEFAULT 0,
  next_review_date DATE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  source_question_id BIGINT,
  KEY idx_wrong_question_user (user_id),
  UNIQUE KEY uk_wrong_question_source (user_id, source_question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS question_bank (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  question_code VARCHAR(50) NOT NULL,
  chapter VARCHAR(100) NOT NULL,
  content TEXT NOT NULL,
  option_a VARCHAR(500) NOT NULL,
  option_b VARCHAR(500) NOT NULL,
  option_c VARCHAR(500) NOT NULL,
  option_d VARCHAR(500) NOT NULL,
  correct_option CHAR(1) NOT NULL,
  explanation TEXT,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_question_code (question_code),
  KEY idx_question_chapter_enabled (chapter, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS practice_attempt (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  chapter VARCHAR(100),
  question_count INT NOT NULL,
  correct_count INT,
  score DECIMAL(5,2),
  submitted TINYINT(1) NOT NULL DEFAULT 0,
  started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  submitted_at DATETIME,
  KEY idx_attempt_user (user_id, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS practice_answer (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  attempt_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  sequence_no INT NOT NULL,
  selected_option CHAR(1),
  correct TINYINT(1),
  UNIQUE KEY uk_attempt_question (attempt_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tutorial_source (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, source_name VARCHAR(100) NOT NULL, source_url VARCHAR(1000) NOT NULL,
  chapter VARCHAR(100) NOT NULL, enabled TINYINT(1) NOT NULL DEFAULT 1, sync_status VARCHAR(20) NOT NULL DEFAULT 'NEVER',
  http_status INT, last_error VARCHAR(1000), last_duration_ms BIGINT, last_synced_at DATETIME,
  last_success_at DATETIME, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tutorial_source_url (source_url(255)), KEY idx_tutorial_source_chapter (chapter)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tutorial_version (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, source_id BIGINT NOT NULL, title VARCHAR(300) NOT NULL,
  summary VARCHAR(1000), content_hash CHAR(64) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  fetched_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, published_at DATETIME,
  KEY idx_tutorial_version_source_status (source_id,status), UNIQUE KEY uk_tutorial_source_hash (source_id,content_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tutorial_section (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, version_id BIGINT NOT NULL, section_key VARCHAR(64) NOT NULL,
  title VARCHAR(300) NOT NULL, content_html MEDIUMTEXT NOT NULL, sequence_no INT NOT NULL,
  UNIQUE KEY uk_tutorial_version_section (version_id,section_key), KEY idx_tutorial_section_version (version_id,sequence_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_tutorial_progress (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, source_id BIGINT NOT NULL,
  section_key VARCHAR(64) NOT NULL, completed TINYINT(1) NOT NULL DEFAULT 1,
  completed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tutorial_progress (user_id,source_id,section_key), KEY idx_tutorial_progress_user (user_id,source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
