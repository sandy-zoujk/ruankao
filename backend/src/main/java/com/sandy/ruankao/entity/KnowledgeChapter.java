package com.sandy.ruankao.entity;

import java.time.LocalDate;
import javax.validation.constraints.*;

/**
 * 软考知识章节及学习掌握情况。
 *
 * @author zoujunke
 */
public class KnowledgeChapter {
  private Long id;
  private Long userId;

  @NotBlank
  @Size(max = 100)
  private String name;

  @NotBlank
  @Size(max = 50)
  private String category;

  @Min(1)
  @Max(5)
  private Integer priority = 3;

  @Min(0)
  @Max(100)
  private Integer mastery = 0;

  @Min(0)
  private Integer questionCount = 0;

  @Min(0)
  private Integer correctCount = 0;

  private LocalDate lastReviewDate;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public Integer getPriority() {
    return priority;
  }

  public void setPriority(Integer priority) {
    this.priority = priority;
  }

  public Integer getMastery() {
    return mastery;
  }

  public void setMastery(Integer mastery) {
    this.mastery = mastery;
  }

  public Integer getQuestionCount() {
    return questionCount;
  }

  public void setQuestionCount(Integer questionCount) {
    this.questionCount = questionCount;
  }

  public Integer getCorrectCount() {
    return correctCount;
  }

  public void setCorrectCount(Integer correctCount) {
    this.correctCount = correctCount;
  }

  public LocalDate getLastReviewDate() {
    return lastReviewDate;
  }

  public void setLastReviewDate(LocalDate lastReviewDate) {
    this.lastReviewDate = lastReviewDate;
  }
}
