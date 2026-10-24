package com.sandy.ruankao.dto;

import javax.validation.constraints.NotBlank;

/** @author zoujunke */
public class UserRoleRequest {
  @NotBlank(message = "角色不能为空")
  private String role;

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }
}
