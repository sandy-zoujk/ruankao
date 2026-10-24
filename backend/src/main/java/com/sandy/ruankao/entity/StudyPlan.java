package com.sandy.ruankao.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.validation.constraints.*;

/**
 * 每日学习计划实体。
 *
 * @author zoujunke
 */
public class StudyPlan {
  private Long id;
  private Long userId;

  @NotBlank
  @Size(max = 120)
  private String title;

  @NotBlank
  @Size(max = 50)
  private String category;

  @NotNull private LocalDate planDate;

  @NotNull
  @Min(1)
  @Max(1440)
  private Integer durationMinutes = 60;

  private Boolean completed = false;

  @Size(max = 500)
  private String notes;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

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

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public LocalDate getPlanDate() {
    return planDate;
  }

  public void setPlanDate(LocalDate planDate) {
    this.planDate = planDate;
  }

  public Integer getDurationMinutes() {
    return durationMinutes;
  }

  public void setDurationMinutes(Integer durationMinutes) {
    this.durationMinutes = durationMinutes;
  }

  public Boolean getCompleted() {
    return completed;
  }

  public void setCompleted(Boolean completed) {
    this.completed = completed;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
