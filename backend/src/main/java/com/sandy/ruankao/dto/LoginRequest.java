package com.sandy.ruankao.dto;

import javax.validation.constraints.*;

/**
 * 用户登录请求参数。
 *
 * @author zoujunke
 */
public class LoginRequest {
  @NotBlank(message = "请输入用户名")
  private String username;

  @NotBlank(message = "请输入密码")
  private String password;
  // AssertTrue 保证请求提交前已经完成前端滑块验证。
  @AssertTrue(message = "请完成滑块验证")
  private boolean sliderVerified;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public boolean isSliderVerified() {
    return sliderVerified;
  }

  public void setSliderVerified(boolean sliderVerified) {
    this.sliderVerified = sliderVerified;
  }
}
