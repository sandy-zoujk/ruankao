package com.sandy.ruankao.logging;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public class TraceIdFilter extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);
  private static final String ATTRIBUTE = TraceIdFilter.class.getName() + ".traceId";

  @Override protected boolean shouldNotFilterAsyncDispatch() { return false; }
  @Override protected boolean shouldNotFilterErrorDispatch() { return false; }

  @Override protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
    Object saved = request.getAttribute(ATTRIBUTE);
    String id = saved instanceof String ? (String) saved
        : TraceContext.resolve(request.getHeader(TraceContext.HEADER));
    request.setAttribute(ATTRIBUTE, id);
    MDC.clear();
    MDC.put(TraceContext.KEY, id);
    response.setHeader(TraceContext.HEADER, id);
    long start = System.nanoTime();
    boolean failed = false;
    try {
      log.info("request start method={} path={} dispatch={}", request.getMethod(),
          request.getRequestURI(), request.getDispatcherType());
      chain.doFilter(request, response);
    } catch (IOException | ServletException | RuntimeException | Error e) {
      failed = true;
      log.error("request failed method={} path={}", request.getMethod(), request.getRequestURI(), LogExceptions.withoutMessages(e));
      throw e;
    } finally {
      try {
        log.info("request dispatch end method={} path={} status={} async={} durationMs={}",
            request.getMethod(), request.getRequestURI(), failed ? 500 : response.getStatus(),
            request.isAsyncStarted(), (System.nanoTime() - start) / 1_000_000);
      } finally { MDC.clear(); }
    }
  }

  @Override protected void doFilterNestedErrorDispatch(HttpServletRequest request,
      HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
    java.util.Map<String, String> previous = MDC.getCopyOfContextMap();
    try { doFilterInternal(request, response, chain); }
    finally { TraceContext.restore(previous); }
  }
}
