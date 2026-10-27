package com.sandy.ruankao.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sandy.ruankao.entity.SysUser;
import com.sandy.ruankao.mapper.SysUserMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户注册、登录及 JWT 身份认证服务。
 *
 * @author zoujunke
 */
@Service
public class AuthService {
  private static final Logger log = LoggerFactory.getLogger(AuthService.class);
  private final SysUserMapper userMapper;
  private final JwtTokenService jwtTokenService;
  private final UserDataService userDataService;
  private final String adminUsername;
  // BCrypt 会为每个密码自动生成盐值，数据库中只保存不可逆的密码摘要。
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  public AuthService(
      SysUserMapper userMapper,
      JwtTokenService jwtTokenService,
      UserDataService userDataService,
      @Value("${app.admin.username:admin}") String adminUsername) {
    this.userMapper = userMapper;
    this.jwtTokenService = jwtTokenService;
    this.userDataService = userDataService;
    this.adminUsername = adminUsername.trim();
  }

  public SysUser register(String username, String password, String nickname) {
    // 去掉用户名两端空格后再查重，避免同一用户名产生视觉重复账号。
    String normalized = username.trim();
    if (userMapper.findByUsername(normalized) != null) {
      throw new IllegalArgumentException("用户名已存在");
    }
    SysUser user = new SysUser();
    user.setUsername(normalized);
    user.setPasswordHash(passwordEncoder.encode(password));
    user.setNickname(nickname.trim());
    user.setRole(
        normalized.equalsIgnoreCase(adminUsername) ? SysUser.ROLE_ADMIN : SysUser.ROLE_USER);
    user.setEnabled(true);
    user.setTokenVersion(0);
    userMapper.insert(user);
    userDataService.provision(user.getId(), SysUser.ROLE_ADMIN.equals(user.getRole()));
    log.info("用户注册处理完成 userId={} role={}", user.getId(), user.getRole());
    return user;
  }

  public Map<String, Object> login(String username, String password) {
    SysUser user = userMapper.findByUsername(username.trim());
    // 使用 BCrypt 对输入密码进行摘要比对，不读取或保存明文密码。
    if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
      log.warn("登录失败 reason=凭证不匹配");
      throw new IllegalArgumentException("用户名或密码错误");
    }
    if (!Boolean.TRUE.equals(user.getEnabled())) {
      log.warn("登录失败 reason=账号停用 userId={}", user.getId());
      throw new IllegalArgumentException("账号已停用，请联系管理员");
    }
    UserSession session = toSession(user);
    // 登录成功后签发带有效期的 JWT，后端不再保存内存会话。
    String token =
        jwtTokenService.createToken(
            user.getId(),
            user.getUsername(),
            user.getNickname(),
            user.getRole(),
            user.getTokenVersion());
    log.info("登录成功 userId={}", user.getId());
    return sessionResult(token, session);
  }

  public boolean isValid(String token) {
    return authenticateToken(token) != null;
  }

  public UserSession getSession(String token) {
    return authenticateToken(token);
  }

  public UserSession authenticateToken(String token) {
    JwtTokenService.JwtUser jwtUser = jwtTokenService.parseToken(token);
    if (jwtUser == null) return null;
    log.debug("校验用户会话 userId={}", jwtUser.getUserId());

    // 账号状态和令牌版本以数据库为准，使停用账号、修改密码能够立即让旧 JWT 失效。
    SysUser user = userMapper.findById(jwtUser.getUserId());
    if (user == null || !Boolean.TRUE.equals(user.getEnabled())) return null;
    if (!Objects.equals(user.getTokenVersion(), jwtUser.getTokenVersion())) return null;
    return toSession(user);
  }

  public void changePassword(Long userId, String oldPassword, String newPassword) {
    SysUser user = userMapper.findById(userId);
    if (user == null || !passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
      throw new IllegalArgumentException("当前密码不正确");
    }
    if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
      throw new IllegalArgumentException("新密码不能与当前密码相同");
    }
    userMapper.updatePassword(userId, passwordEncoder.encode(newPassword));
    log.info("密码修改处理完成 userId={}", userId);
  }

  public Map<String, Object> sessionResult(String token, UserSession session) {
    Map<String, Object> result = profileResult(session);
    result.put("token", token);
    result.put("expiresIn", jwtTokenService.getExpirationSeconds());
    return result;
  }

  public Map<String, Object> profileResult(UserSession session) {
    Map<String, Object> result = new LinkedHashMap<String, Object>();
    result.put("userId", session.getUserId());
    result.put("username", session.getUsername());
    result.put("nickname", session.getNickname());
    result.put("role", session.getRole());
    boolean admin = SysUser.ROLE_ADMIN.equals(session.getRole());
    result.put("roleLabel", admin ? "管理员" : "普通用户");
    result.put("permissionSummary", admin ? "全部权限" : "学习权限");
    result.put("enabled", session.getEnabled());
    return result;
  }

  private UserSession toSession(SysUser user) {
    return new UserSession(
        user.getId(),
        user.getUsername(),
        user.getNickname(),
        user.getRole(),
        user.getEnabled(),
        user.getTokenVersion());
  }

  /**
   * 登录后的最小用户会话信息。
   *
   * @author zoujunke
   */
  public static class UserSession {
    private final Long userId;
    private final String username;
    private final String nickname;
    private final String role;
    private final Boolean enabled;
    private final Integer tokenVersion;

    public UserSession(
        Long userId,
        String username,
        String nickname,
        String role,
        Boolean enabled,
        Integer tokenVersion) {
      this.userId = userId;
      this.username = username;
      this.nickname = nickname;
      this.role = role;
      this.enabled = enabled;
      this.tokenVersion = tokenVersion;
    }

    public Long getUserId() {
      return userId;
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

    public Integer getTokenVersion() {
      return tokenVersion;
    }
  }
}
