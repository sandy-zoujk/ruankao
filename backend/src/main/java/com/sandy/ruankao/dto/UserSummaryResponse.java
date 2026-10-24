package com.sandy.ruankao.dto;

import com.sandy.ruankao.entity.SysUser;
import java.time.LocalDateTime;

/**
 * 用户管理列表响应，不包含密码摘要等敏感字段。
 *
 * @author zoujunke
 */
public class UserSummaryResponse {
  private final Long id;
  private final String username;
  private final String nickname;
  private final String role;
  private final Boolean enabled;
  private final LocalDateTime createdAt;
  private final boolean defaultAdmin;

  public UserSummaryResponse(SysUser user) {
    this(user, false);
  }

  public UserSummaryResponse(SysUser user, boolean defaultAdmin) {
    this.id = user.getId();
    this.username = user.getUsername();
    this.nickname = user.getNickname();
    this.role = user.getRole();
    this.enabled = user.getEnabled();
    this.createdAt = user.getCreatedAt();
    this.defaultAdmin = defaultAdmin;
  }

  public Long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getNickname() {
    return nickname;
  }

  public String getRole() {
    return role;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public boolean isDefaultAdmin() {
    return defaultAdmin;
  }
}
