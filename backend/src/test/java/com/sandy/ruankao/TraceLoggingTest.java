package com.sandy.ruankao;

import com.sandy.ruankao.logging.*;
import java.util.*;
import java.util.concurrent.*;
import javax.servlet.DispatcherType;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.slf4j.MDC;
import org.springframework.mock.web.*;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import org.springframework.web.client.RestTemplate;

class TraceLoggingTest {
  @AfterEach void clean() { MDC.clear(); }

  @Test void requestHeadersErrorsAndRedispatch() throws Exception {
    TraceIdFilter filter = new TraceIdFilter();
    for (String incoming : Arrays.asList(null, "client-123", "bad value")) {
      MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/software/info");
      if (incoming != null) request.addHeader(TraceContext.HEADER, incoming);
      MockHttpServletResponse response = new MockHttpServletResponse();
      filter.doFilter(request, response, (req, res) -> {
        assertEquals(TraceContext.current(), response.getHeader(TraceContext.HEADER));
        if ("client-123".equals(incoming)) assertEquals(incoming, TraceContext.current());
        else assertTrue(TraceContext.current().matches("[0-9a-f]{32}"));
      });
      assertNull(MDC.getCopyOfContextMap());
      String original = response.getHeader(TraceContext.HEADER);
      request.setDispatcherType(DispatcherType.ERROR);
      filter.doFilter(request, response, (req, res) -> assertEquals(original, TraceContext.current()));
      assertNull(TraceContext.current());
    }
    MockHttpServletResponse response = new MockHttpServletResponse();
    assertThrows(IllegalStateException.class, () -> filter.doFilter(
        new MockHttpServletRequest(), response, (req, res) -> { throw new IllegalStateException("test"); }));
    assertNotNull(response.getHeader(TraceContext.HEADER));
    assertNull(TraceContext.current());
  }

  @Test void reusedWorkersAndParentCompletionNeverLeak() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      List<Future<?>> results = new ArrayList<>();
      CountDownLatch start = new CountDownLatch(1);
      for (int i = 0; i < 100; i++) {
        String id = "request-" + i;
        MDC.put(TraceContext.KEY, id);
        Runnable task = new MdcTaskDecorator().decorate(() -> {
          try { assertTrue(start.await(5, TimeUnit.SECONDS)); }
          catch (InterruptedException e) { throw new RuntimeException(e); }
          assertEquals(id, TraceContext.current());
        });
        results.add(pool.submit(task));
        MDC.clear();
      }
      start.countDown();
      for (Future<?> result : results) result.get(10, TimeUnit.SECONDS);
      for (int i = 0; i < 100; i++)
        assertNull(pool.submit(() -> TraceContext.current()).get(5, TimeUnit.SECONDS));
      Runnable failing = TraceContext.wrap(() -> { throw new IllegalStateException("test"); });
      assertThrows(ExecutionException.class, () -> pool.submit(failing).get(5, TimeUnit.SECONDS));
      assertNull(pool.submit(() -> TraceContext.current()).get(5, TimeUnit.SECONDS));
    } finally { pool.shutdownNow(); }
  }

  @Test void nestedTaskAndIndependentScopesRestoreContext() throws Exception {
    MDC.put(TraceContext.KEY, "parent");
    Runnable task = TraceContext.wrap(() -> assertEquals("parent", TraceContext.current()));
    MDC.put(TraceContext.KEY, "caller");
    task.run();
    assertEquals("caller", TraceContext.current());
    try (TraceContext.Scope ignored = TraceContext.open(null)) {
      assertTrue(TraceContext.current().matches("[0-9a-f]{32}"));
      assertEquals(TraceContext.current(), TraceContext.wrapCallable(TraceContext::current).call());
    }
    assertEquals("caller", TraceContext.current());
    MDC.clear();
    TraceContext.wrap(() -> assertNotNull(TraceContext.current())).run();
    assertNull(TraceContext.current());
  }

  @Test void outboundHttpOverridesStaleHeader() {
    RestTemplate rest = new RestTemplate();
    rest.getInterceptors().add(new TraceIdHttpInterceptor());
    MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
    server.expect(requestTo("http://local.test/resource"))
        .andExpect(header(TraceContext.HEADER, "parent"))
        .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));
    MDC.put(TraceContext.KEY, "parent");
    HttpHeaders headers = new HttpHeaders();
    headers.set(TraceContext.HEADER, "stale");
    assertEquals("ok", rest.exchange("http://local.test/resource", HttpMethod.GET,
        new HttpEntity<Void>(headers), String.class).getBody());
    assertEquals("parent", TraceContext.current());
    server.verify();
  }
}
