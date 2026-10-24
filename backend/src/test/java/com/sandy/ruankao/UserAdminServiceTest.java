package com.sandy.ruankao;

import com.sandy.ruankao.entity.SysUser;
import com.sandy.ruankao.mapper.SysUserMapper;
import com.sandy.ruankao.service.UserAdminService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 管理员账号状态操作测试。
 *
 * @author zoujunke
 */
class UserAdminServiceTest {
    @Test
    void shouldNotAllowAdminToDisableSelf() {
        SysUserMapper mapper = mock(SysUserMapper.class);
        SysUser admin = new SysUser();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setRole(SysUser.ROLE_ADMIN);
        admin.setEnabled(true);
        when(mapper.findById(1L)).thenReturn(admin);

        UserAdminService service = new UserAdminService(mapper);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.updateStatus(1L, 1L, false));

        assertEquals("不能停用当前管理员账号", exception.getMessage());
        verify(mapper, never()).updateStatus(anyLong(), anyBoolean());
    }
}
