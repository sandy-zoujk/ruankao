package com.sandy.ruankao.mapper;

import com.sandy.ruankao.entity.StudyPlan;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 学习计划数据访问接口，SQL 定义在对应的 MyBatis XML 文件中。
 *
 * @author zoujunke
 */
@Mapper
public interface StudyPlanMapper {
  List<StudyPlan> findAll(@Param("userId") Long userId);

  StudyPlan findById(@Param("id") Long id, @Param("userId") Long userId);

  List<StudyPlan> findBetween(
      @Param("userId") Long userId, @Param("start") LocalDate start, @Param("end") LocalDate end);

  long count(@Param("userId") Long userId);

  long countByCompleted(@Param("userId") Long userId, @Param("completed") boolean completed);

  int insert(StudyPlan plan);

  int update(StudyPlan plan);

  int toggle(@Param("id") Long id, @Param("userId") Long userId);

  int delete(@Param("id") Long id, @Param("userId") Long userId);
}
