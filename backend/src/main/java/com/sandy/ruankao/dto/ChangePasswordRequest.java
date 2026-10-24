package com.sandy.ruankao.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 当前用户修改密码请求。
 *
 * @author zoujunke
 */
public class ChangePasswordRequest {
    @NotBlank(message = "请输入当前密码")
    private String oldPassword;

    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 50, message = "新密码长度须为6-50位")
    private String newPassword;

    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
