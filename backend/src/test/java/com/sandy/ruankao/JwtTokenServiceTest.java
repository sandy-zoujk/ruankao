package com.sandy.ruankao;

import static org.junit.jupiter.api.Assertions.*;

import com.sandy.ruankao.service.JwtTokenService;
import org.junit.jupiter.api.Test;

/**
 * JWT 签发、解析和防篡改单元测试。
 *
 * @author zoujunke
 */
class JwtTokenServiceTest {
  private static final String TEST_SECRET = "test-jwt-secret-must-be-at-least-32-bytes-long";

  @Test
  void shouldCreateAndParseValidToken() {
    JwtTokenService service = new JwtTokenService(TEST_SECRET, 60);
    String token = service.createToken(1L, "zoujunke", "Zou", "ADMIN", 3);

    JwtTokenService.JwtUser user = service.parseToken(token);
    assertNotNull(user);
    assertEquals(1L, user.getUserId());
    assertEquals("zoujunke", user.getUsername());
    assertEquals("Zou", user.getNickname());
    assertEquals("ADMIN", user.getRole());
    assertEquals(3, user.getTokenVersion());
  }

  @Test
  void shouldRejectTamperedToken() {
    JwtTokenService service = new JwtTokenService(TEST_SECRET, 60);
    String token = service.createToken(1L, "zoujunke", "Zou", "USER", 0);

    // 修改签名段的第一个字符会破坏签名，解析结果必须为空。
    int signatureStart = token.lastIndexOf('.') + 1;
    char original = token.charAt(signatureStart);
    char replacement = original == 'a' ? 'b' : 'a';
    String tamperedToken =
        token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1);
    assertNull(service.parseToken(tamperedToken));
  }
}
