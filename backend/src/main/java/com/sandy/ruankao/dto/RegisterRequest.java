package com.sandy.ruankao.dto;

import javax.validation.constraints.*;

/**
 * 用户注册请求参数及字段校验规则。
 *
 * @author zoujunke
 */
public class RegisterRequest {
  @NotBlank(message = "请输入用户名")
  @Pattern(regexp = "^[A-Za-z0-9_]{4,30}$", message = "用户名须为4-30位字母、数字或下划线")
  private String username;

  @NotBlank(message = "请输入密码")
  @Size(min = 6, max = 50, message = "密码长度须为6-50位")
  private String password;

  @NotBlank(message = "请输入昵称")
  @Size(max = 50, message = "昵称不能超过50个字符")
  private String nickname;
  // 注册与登录使用相同的滑块完成标记，未验证的请求会被参数校验拒绝。
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

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  public boolean isSliderVerified() {
    return sliderVerified;
  }

  public void setSliderVerified(boolean sliderVerified) {
    this.sliderVerified = sliderVerified;
  }
}
