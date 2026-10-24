package com.sandy.ruankao.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * JWT 的签发与验证服务。
 *
 * @author zoujunke
 */
@Service
public class JwtTokenService {
  private static final String ISSUER = "ruankao-study-system";

  private final SecretKey signingKey;
  private final long expirationMillis;

  public JwtTokenService(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.expiration-minutes:120}") long expirationMinutes) {
    byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
    if (secretBytes.length < 32) {
      throw new IllegalArgumentException("JWT 密钥长度不能少于 32 字节");
    }
    if (expirationMinutes < 1) {
      throw new IllegalArgumentException("JWT 有效期必须大于 0 分钟");
    }
    this.signingKey = Keys.hmacShaKeyFor(secretBytes);
    this.expirationMillis = expirationMinutes * 60L * 1000L;
  }

  public String createToken(
      Long userId, String username, String nickname, String role, Integer tokenVersion) {
    Date issuedAt = new Date();
    Date expiresAt = new Date(issuedAt.getTime() + expirationMillis);

    // JWT 只保存识别当前用户所需的最小信息，绝不放入密码摘要等敏感数据。
    return Jwts.builder()
        .setIssuer(ISSUER)
        .setSubject(String.valueOf(userId))
        .claim("username", username)
        .claim("nickname", nickname)
        .claim("role", role)
        .claim("tokenVersion", tokenVersion)
        .setIssuedAt(issuedAt)
        .setExpiration(expiresAt)
        .signWith(signingKey, SignatureAlgorithm.HS256)
        .compact();
  }

  public JwtUser parseToken(String token) {
    if (token == null || token.trim().isEmpty()) return null;
    try {
      // parseClaimsJws 会同时校验签名、签发方和过期时间，任何一项不合法都拒绝访问。
      Claims claims =
          Jwts.parserBuilder()
              .setSigningKey(signingKey)
              .requireIssuer(ISSUER)
              .build()
              .parseClaimsJws(token)
              .getBody();
      return new JwtUser(
          Long.valueOf(claims.getSubject()),
          claims.get("username", String.class),
          claims.get("nickname", String.class),
          claims.get("role", String.class),
          claims.get("tokenVersion", Integer.class));
    } catch (JwtException | IllegalArgumentException exception) {
      return null;
    }
  }

  public long getExpirationSeconds() {
    return expirationMillis / 1000L;
  }

  /**
   * 从合法 JWT 中解析出的用户信息。
   *
   * @author zoujunke
   */
  public static class JwtUser {
    private final Long userId;
    private final String username;
    private final String nickname;
    private final String role;
    private final Integer tokenVersion;

    public JwtUser(
        Long userId, String username, String nickname, String role, Integer tokenVersion) {
      this.userId = userId;
      this.username = username;
      this.nickname = nickname;
      this.role = role;
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

    public Integer getTokenVersion() {
      return tokenVersion;
    }
  }
}
