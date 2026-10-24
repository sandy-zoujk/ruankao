package com.sandy.ruankao.service;

import com.sandy.ruankao.dto.UserSummaryResponse;
import com.sandy.ruankao.entity.SysUser;
import com.sandy.ruankao.mapper.SysUserMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 管理员用户查询与账号状态管理服务。
 *
 * @author zoujunke
 */
@Service
public class UserAdminService {
  private final SysUserMapper userMapper;
  private final String defaultAdminUsername;

  public UserAdminService(
      SysUserMapper userMapper, @Value("${app.admin.username:admin}") String defaultAdminUsername) {
    this.userMapper = userMapper;
    this.defaultAdminUsername = defaultAdminUsername.trim();
  }

  public Map<String, Object> list(
      String keyword, Boolean enabled, String role, int page, int size) {
    page = Math.max(1, page);
    size = Math.max(1, Math.min(100, size));
    String normalized = keyword == null ? null : keyword.trim();
    String normalizedRole = normalizeRole(role, true);
    List<UserSummaryResponse> result = new ArrayList<UserSummaryResponse>();
    for (SysUser user :
        userMapper.findPage(normalized, enabled, normalizedRole, size, (long) (page - 1) * size)) {
      result.add(summary(user));
    }
    Map<String, Object> response = new LinkedHashMap<String, Object>();
    response.put("items", result);
    response.put("total", userMapper.countFiltered(normalized, enabled, normalizedRole));
    response.put("page", page);
    response.put("size", size);
    return response;
  }

  public UserSummaryResponse updateStatus(Long operatorId, Long targetId, boolean enabled) {
    SysUser target = userMapper.findById(targetId);
    if (target == null) throw new IllegalArgumentException("用户不存在");
    if (Boolean.valueOf(enabled).equals(target.getEnabled())) return summary(target);
    if (operatorId.equals(targetId) && !enabled) {
      throw new IllegalArgumentException("不能停用当前管理员账号");
    }
    if (!enabled && isDefaultAdmin(target)) throw new IllegalArgumentException("不能停用默认管理员账号");
    if (!enabled
        && SysUser.ROLE_ADMIN.equals(target.getRole())
        && userMapper.countEnabledAdmins() <= 1) {
      throw new IllegalArgumentException("系统必须至少保留一个启用的管理员");
    }
    userMapper.updateStatus(targetId, enabled);
    return summary(userMapper.findById(targetId));
  }

  public UserSummaryResponse updateRole(Long operatorId, Long targetId, String role) {
    String normalizedRole = normalizeRole(role, false);
    SysUser target = userMapper.findById(targetId);
    if (target == null) throw new IllegalArgumentException("用户不存在");
    if (normalizedRole.equals(target.getRole())) return summary(target);
    if (operatorId.equals(targetId) && SysUser.ROLE_USER.equals(normalizedRole)) {
      throw new IllegalArgumentException("不能降级当前管理员账号");
    }
    if (isDefaultAdmin(target) && SysUser.ROLE_USER.equals(normalizedRole)) {
      throw new IllegalArgumentException("不能降级默认管理员账号");
    }
    if (SysUser.ROLE_ADMIN.equals(target.getRole())
        && SysUser.ROLE_USER.equals(normalizedRole)
        && Boolean.TRUE.equals(target.getEnabled())
        && userMapper.countEnabledAdmins() <= 1) {
      throw new IllegalArgumentException("系统必须至少保留一个启用的管理员");
    }
    userMapper.updateRole(targetId, normalizedRole);
    return summary(userMapper.findById(targetId));
  }

  private UserSummaryResponse summary(SysUser user) {
    return new UserSummaryResponse(user, isDefaultAdmin(user));
  }

  private boolean isDefaultAdmin(SysUser user) {
    return user.getUsername() != null && user.getUsername().equalsIgnoreCase(defaultAdminUsername);
  }

  private String normalizeRole(String role, boolean optional) {
    if (role == null || role.trim().isEmpty()) {
      if (optional) return null;
      throw new IllegalArgumentException("角色不能为空");
    }
    String value = role.trim().toUpperCase();
    if (!SysUser.ROLE_ADMIN.equals(value) && !SysUser.ROLE_USER.equals(value)) {
      throw new IllegalArgumentException("角色只能是 ADMIN 或 USER");
    }
    return value;
  }
}
