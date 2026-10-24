package com.sandy.ruankao.entity;

import javax.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 错题及复习进度实体。
 *
 * @author zoujunke
 */
public class WrongQuestion {
    private Long id;
    private Long userId;
    @NotBlank @Size(max = 200)
    private String title;
    @NotBlank
    private String content;
    private String correctAnswer;
    private String myAnswer;
    @NotBlank @Size(max = 50)
    private String errorReason;
    private KnowledgeChapter chapter;
    private Integer reviewCount = 0;
    private Boolean mastered = false;
    private LocalDate nextReviewDate;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    public String getMyAnswer() { return myAnswer; }
    public void setMyAnswer(String myAnswer) { this.myAnswer = myAnswer; }
    public String getErrorReason() { return errorReason; }
    public void setErrorReason(String errorReason) { this.errorReason = errorReason; }
    public KnowledgeChapter getChapter() { return chapter; }
    public void setChapter(KnowledgeChapter chapter) { this.chapter = chapter; }
    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }
    public Boolean getMastered() { return mastered; }
    public void setMastered(Boolean mastered) { this.mastered = mastered; }
    public LocalDate getNextReviewDate() { return nextReviewDate; }
    public void setNextReviewDate(LocalDate nextReviewDate) { this.nextReviewDate = nextReviewDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
