package com.sandy.ruankao.mapper;

import com.sandy.ruankao.entity.WrongQuestion;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 错题数据访问接口，SQL 定义在对应的 MyBatis XML 文件中。
 *
 * @author zoujunke
 */
@Mapper
public interface WrongQuestionMapper {
  List<WrongQuestion> findAll(@Param("userId") Long userId);

  WrongQuestion findById(@Param("id") Long id, @Param("userId") Long userId);

  long count(@Param("userId") Long userId);

  long countByMastered(@Param("userId") Long userId, @Param("mastered") boolean mastered);

  int insert(WrongQuestion question);

  int updateReview(WrongQuestion question);

  int delete(@Param("id") Long id, @Param("userId") Long userId);
}
