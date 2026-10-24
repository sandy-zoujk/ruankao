package com.sandy.ruankao.service;

import com.sandy.ruankao.entity.KnowledgeChapter;
import com.sandy.ruankao.mapper.KnowledgeChapterMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户学习数据初始化及旧数据归属服务。
 *
 * @author zoujunke
 */
@Service
public class UserDataService {
  private static final String[][] DEFAULT_CHAPTERS = {
    {"项目整合管理", "项目管理", "4"}, {"项目范围管理", "项目管理", "5"},
    {"项目进度管理", "项目管理", "5"}, {"项目成本管理", "项目管理", "5"},
    {"项目质量管理", "项目管理", "4"}, {"项目资源管理", "项目管理", "3"},
    {"项目沟通管理", "项目管理", "4"}, {"项目风险管理", "项目管理", "5"},
    {"项目采购管理", "项目管理", "3"}, {"项目干系人管理", "项目管理", "4"},
    {"信息系统与新技术", "综合知识", "3"}, {"法律法规与标准", "综合知识", "3"},
    {"案例计算专题", "案例分析", "5"}, {"论文写作专题", "论文", "5"}
  };

  private final JdbcTemplate jdbcTemplate;
  private final KnowledgeChapterMapper chapterMapper;

  public UserDataService(JdbcTemplate jdbcTemplate, KnowledgeChapterMapper chapterMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.chapterMapper = chapterMapper;
  }

  @Transactional
  public void provision(Long userId, boolean claimLegacyData) {
    if (claimLegacyData) {
      // 历史版本没有 user_id，首次识别到管理员后统一将旧数据归属管理员。
      jdbcTemplate.update("UPDATE study_plan SET user_id = ? WHERE user_id IS NULL", userId);
      jdbcTemplate.update("UPDATE knowledge_chapter SET user_id = ? WHERE user_id IS NULL", userId);
      jdbcTemplate.update("UPDATE wrong_question SET user_id = ? WHERE user_id IS NULL", userId);
    }
    if (chapterMapper.count(userId) > 0) return;

    for (String[] item : DEFAULT_CHAPTERS) {
      KnowledgeChapter chapter = new KnowledgeChapter();
      chapter.setUserId(userId);
      chapter.setName(item[0]);
      chapter.setCategory(item[1]);
      chapter.setPriority(Integer.valueOf(item[2]));
      chapterMapper.insert(chapter);
    }
  }
}
