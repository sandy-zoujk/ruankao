CREATE DATABASE IF NOT EXISTS ruankao_study
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE ruankao_study;

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
  KEY idx_wrong_question_user (user_id),
  CONSTRAINT fk_wrong_question_chapter FOREIGN KEY (chapter_id)
    REFERENCES knowledge_chapter(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
