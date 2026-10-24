package com.sandy.ruankao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sandy.ruankao.config.AdminInterceptor;
import com.sandy.ruankao.config.AuthInterceptor;
import com.sandy.ruankao.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 管理员接口权限拦截测试。
 *
 * @author zoujunke
 */
class AdminInterceptorTest {
    private final AdminInterceptor interceptor = new AdminInterceptor(new ObjectMapper());

    @Test
    void shouldRejectNormalUser() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE,
                new AuthService.UserSession(2L, "student", "学生", "USER", true, 0));

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(403, response.getStatus());
    }

    @Test
    void shouldAllowAdmin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE,
                new AuthService.UserSession(1L, "admin", "管理员", "ADMIN", true, 0));

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }
}
