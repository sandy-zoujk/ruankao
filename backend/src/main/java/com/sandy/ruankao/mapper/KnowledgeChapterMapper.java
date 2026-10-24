package com.sandy.ruankao.mapper;

import com.sandy.ruankao.entity.KnowledgeChapter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 知识章节数据访问接口，SQL 定义在对应的 MyBatis XML 文件中。
 *
 * @author zoujunke
 */
@Mapper
public interface KnowledgeChapterMapper {
    List<KnowledgeChapter> findAll(@Param("userId") Long userId);

    KnowledgeChapter findById(@Param("id") Long id, @Param("userId") Long userId);

    long count(@Param("userId") Long userId);

    int insert(KnowledgeChapter chapter);

    int update(KnowledgeChapter chapter);
}
