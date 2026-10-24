package com.sandy.ruankao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.sandy.ruankao.entity.SysUser;
import com.sandy.ruankao.mapper.SysUserMapper;
import com.sandy.ruankao.service.AuthService;
import com.sandy.ruankao.service.JwtTokenService;
import com.sandy.ruankao.service.UserDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 用户登录、密码修改和 JWT 状态校验测试。
 *
 * @author zoujunke
 */
class AuthServiceTest {
  private SysUserMapper userMapper;
  private JwtTokenService jwtTokenService;
  private AuthService authService;

  @BeforeEach
  void setUp() {
    userMapper = mock(SysUserMapper.class);
    jwtTokenService = mock(JwtTokenService.class);
    authService =
        new AuthService(userMapper, jwtTokenService, mock(UserDataService.class), "admin");
  }

  @Test
  void shouldRejectDisabledUserAfterPasswordIsVerified() {
    SysUser user = user("student", "password123", false, 0);
    when(userMapper.findByUsername("student")).thenReturn(user);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> authService.login("student", "password123"));
    assertEquals("账号已停用，请联系管理员", exception.getMessage());
    verify(jwtTokenService, never())
        .createToken(anyLong(), anyString(), anyString(), anyString(), anyInt());
  }

  @Test
  void shouldRejectJwtAfterTokenVersionChanges() {
    SysUser user = user("student", "password123", true, 2);
    when(jwtTokenService.parseToken("old-token"))
        .thenReturn(new JwtTokenService.JwtUser(1L, "student", "学生", "USER", 1));
    when(userMapper.findById(1L)).thenReturn(user);

    assertNull(authService.authenticateToken("old-token"));
  }

  @Test
  void shouldHashNewPasswordAndIncrementTokenVersion() {
    SysUser user = user("student", "oldPassword", true, 0);
    when(userMapper.findById(1L)).thenReturn(user);

    authService.changePassword(1L, "oldPassword", "newPassword");

    verify(userMapper)
        .updatePassword(
            eq(1L), argThat(hash -> new BCryptPasswordEncoder().matches("newPassword", hash)));
  }

  private SysUser user(String username, String password, boolean enabled, int tokenVersion) {
    SysUser user = new SysUser();
    user.setId(1L);
    user.setUsername(username);
    user.setNickname("学生");
    user.setPasswordHash(new BCryptPasswordEncoder().encode(password));
    user.setRole(SysUser.ROLE_USER);
    user.setEnabled(enabled);
    user.setTokenVersion(tokenVersion);
    return user;
  }
}
