package com.sandy.ruankao;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sandy.ruankao.config.*;
import com.sandy.ruankao.entity.SysUser;
import com.sandy.ruankao.logging.TraceContext;
import com.sandy.ruankao.mapper.SysUserMapper;
import com.sandy.ruankao.service.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.mock.web.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BusinessLoggingTest {
  private final List<Logger> attached = new ArrayList<>();
  private final ListAppender<ILoggingEvent> appender = new ListAppender<ILoggingEvent>() {
    @Override protected void append(ILoggingEvent event) {
      event.prepareForDeferredProcessing();
      super.append(event);
    }
  };

  @BeforeEach void start() {
    appender.start();
    MDC.put(TraceContext.KEY, "business-test-123");
  }

  private void capture(Class<?> type) {
    Logger logger = (Logger) LoggerFactory.getLogger(type);
    logger.addAppender(appender);
    attached.add(logger);
  }

  @AfterEach void stop() {
    attached.forEach(logger -> logger.detachAppender(appender));
    appender.stop();
    MDC.clear();
  }

  private ILoggingEvent event(String text, Level level) {
    ILoggingEvent event = appender.list.stream()
        .filter(e -> e.getFormattedMessage().contains(text)).findFirst()
        .orElseThrow(() -> new AssertionError("Missing event: " + text));
    assertEquals(level, event.getLevel());
    assertEquals("business-test-123", event.getMDCPropertyMap().get(TraceContext.KEY));
    return event;
  }

  private void noSecrets(String... secrets) {
    for (ILoggingEvent event : appender.list) {
      String output = event.getFormattedMessage();
      ch.qos.logback.classic.spi.IThrowableProxy failure = event.getThrowableProxy();
      while (failure != null) {
        output += failure.getClassName() + failure.getMessage();
        failure = failure.getCause();
      }
      for (String secret : secrets) assertFalse(output.contains(secret), output);
    }
  }

  @Test void loginSuccessAndFailureOnlyLogSafeIdentifiers() {
    capture(AuthService.class);
    SysUserMapper users = mock(SysUserMapper.class);
    JwtTokenService jwt = mock(JwtTokenService.class);
    AuthService auth = new AuthService(users, jwt, mock(UserDataService.class), "admin");
    SysUser user = new SysUser();
    user.setId(7L);
    user.setUsername("private-user");
    user.setNickname("private-nickname");
    user.setEnabled(true);
    user.setRole("USER");
    user.setTokenVersion(0);
    String hash = new BCryptPasswordEncoder().encode("private-password");
    user.setPasswordHash(hash);
    when(users.findByUsername("private-user")).thenReturn(user);
    when(jwt.createToken(anyLong(), anyString(), anyString(), anyString(), anyInt()))
        .thenReturn("private-jwt");
    auth.login("private-user", "private-password");
    assertTrue(event("登录成功", Level.INFO).getFormattedMessage().contains("userId=7"));
    assertThrows(IllegalArgumentException.class, () -> auth.login("private-user", "wrong-password"));
    event("登录失败", Level.WARN);
    noSecrets("private-user", "private-password", "private-nickname", "private-jwt", hash, "wrong-password");
  }

  @Test void authorizationDenialAndValidationDoNotLogRawInput() throws Exception {
    capture(AuthInterceptor.class);
    capture(AdminInterceptor.class);
    capture(GlobalExceptionHandler.class);
    AuthService auth = mock(AuthService.class);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer private-token");
    assertFalse(new AuthInterceptor(auth, new ObjectMapper())
        .preHandle(request, new MockHttpServletResponse(), new Object()));
    event("鉴权拒绝", Level.WARN);
    assertFalse(new AdminInterceptor(new ObjectMapper())
        .preHandle(request, new MockHttpServletResponse(), new Object()));
    event("权限拒绝", Level.WARN);
    new GlobalExceptionHandler().handleIllegalArgument(new IllegalArgumentException("private-input"));
    event("业务校验未通过", Level.WARN);
    noSecrets("private-token", "private-input");
  }

  @Test void convertedSyncFailureRetainsFramesWithoutSensitiveMessages() throws Exception {
    capture(TutorialService.class);
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    TutorialFetchService fetch = mock(TutorialFetchService.class);
    String url = "https://example.com/source?token=private-query";
    Map<String, Object> source = new HashMap<>();
    source.put("sourceUrl", url);
    when(jdbc.query(anyString(), org.mockito.ArgumentMatchers.<RowMapper<Map<String, Object>>>any(), eq(9L)))
        .thenReturn(Collections.singletonList(source));
    IllegalStateException failure = new IllegalStateException("private-sql " + url,
        new IllegalArgumentException("private-password"));
    when(fetch.fetch(url)).thenThrow(failure);
    assertThrows(IllegalArgumentException.class, () -> new TutorialService(jdbc, fetch).sync(9L));
    ILoggingEvent error = event("教程同步失败", Level.ERROR);
    assertTrue(error.getFormattedMessage().contains("sourceId=9"));
    assertNotNull(error.getThrowableProxy().getCause());
    assertEquals(failure.getStackTrace().length, error.getThrowableProxy().getStackTraceElementProxyArray().length);
    noSecrets("private-query", "private-password", "private-sql", url);
  }

  @Test void ordinaryQueriesStayBelowInfo() {
    capture(PracticeService.class);
    Logger logger = (Logger) LoggerFactory.getLogger(PracticeService.class);
    Level previous = logger.getLevel();
    try {
      logger.setLevel(Level.INFO);
      new PracticeService(mock(JdbcTemplate.class)).chapters();
      assertTrue(appender.list.isEmpty());
      logger.setLevel(Level.DEBUG);
      new PracticeService(mock(JdbcTemplate.class)).chapters();
      event("查询练习章节", Level.DEBUG);
    } finally { logger.setLevel(previous); }
  }
}
