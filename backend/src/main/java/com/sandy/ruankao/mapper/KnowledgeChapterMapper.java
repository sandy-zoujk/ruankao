package com.sandy.ruankao.mapper;

import com.sandy.ruankao.entity.KnowledgeChapter;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 知识章节数据访问接口，SQL 定义在对应的 MyBatis XML 文件中。
 *
 * @author zoujunke
 */
@Mapper
public interface KnowledgeChapterMapper {
  List<KnowledgeChapter> findAll(@Param("userId") Long userId);

  List<KnowledgeChapter> findPage(
      @Param("userId") Long userId, @Param("limit") int limit, @Param("offset") long offset);

  KnowledgeChapter findById(@Param("id") Long id, @Param("userId") Long userId);

  long count(@Param("userId") Long userId);

  int insert(KnowledgeChapter chapter);

  int update(KnowledgeChapter chapter);
}
