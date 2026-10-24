package com.sandy.ruankao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sandy.ruankao.entity.SysUser;
import com.sandy.ruankao.mapper.SysUserMapper;
import com.sandy.ruankao.service.UserAdminService;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 管理员账号状态操作测试。
 *
 * @author zoujunke
 */
class UserAdminServiceTest {
  @Test
  void shouldPageAndClampUserQueryParameters() {
    SysUserMapper mapper = mock(SysUserMapper.class);
    when(mapper.findPage("sandy", true, SysUser.ROLE_USER, 100, 200L))
        .thenReturn(Collections.<SysUser>emptyList());
    when(mapper.countFiltered("sandy", true, SysUser.ROLE_USER)).thenReturn(205L);

    Map<String, Object> result =
        new UserAdminService(mapper, "admin").list(" sandy ", true, "user", 3, 500);

    assertEquals(205L, result.get("total"));
    assertEquals(3, result.get("page"));
    assertEquals(100, result.get("size"));
    verify(mapper).findPage("sandy", true, SysUser.ROLE_USER, 100, 200L);
  }

  @Test
  void shouldNotAllowAdminToDisableSelf() {
    SysUserMapper mapper = mock(SysUserMapper.class);
    SysUser admin = new SysUser();
    admin.setId(1L);
    admin.setUsername("admin");
    admin.setRole(SysUser.ROLE_ADMIN);
    admin.setEnabled(true);
    when(mapper.findById(1L)).thenReturn(admin);

    UserAdminService service = new UserAdminService(mapper, "admin");
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> service.updateStatus(1L, 1L, false));

    assertEquals("不能停用当前管理员账号", exception.getMessage());
    verify(mapper, never()).updateStatus(anyLong(), anyBoolean());
  }

  @Test
  void shouldProtectDefaultAndLastEnabledAdmin() {
    SysUserMapper mapper = mock(SysUserMapper.class);
    SysUser admin = new SysUser();
    admin.setId(1L);
    admin.setUsername("admin");
    admin.setRole(SysUser.ROLE_ADMIN);
    admin.setEnabled(true);
    when(mapper.findById(1L)).thenReturn(admin);
    when(mapper.countEnabledAdmins()).thenReturn(1L);
    UserAdminService service = new UserAdminService(mapper, "admin");
    assertThrows(
        IllegalArgumentException.class, () -> service.updateRole(2L, 1L, SysUser.ROLE_USER));
    assertThrows(IllegalArgumentException.class, () -> service.updateStatus(2L, 1L, false));
    verify(mapper, never()).updateRole(anyLong(), anyString());
  }
}
