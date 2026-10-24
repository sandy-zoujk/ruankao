package com.sandy.ruankao.dto;

import javax.validation.constraints.NotNull;

/**
 * 管理员启用或停用账号请求。
 *
 * @author zoujunke
 */
public class UserStatusRequest {
    @NotNull(message = "请指定账号状态")
    private Boolean enabled;

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
