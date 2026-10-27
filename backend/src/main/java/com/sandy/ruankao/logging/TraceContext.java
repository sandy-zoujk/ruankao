package com.sandy.ruankao.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.slf4j.MDC;

/** Log correlation only; no spans or tracing SDK. */
public final class TraceContext {
  public static final String HEADER = "X-Trace-Id";
  public static final String KEY = "traceId";

  private TraceContext() {}

  public static String resolve(String value) {
    return value != null && value.matches("[A-Za-z0-9_-]{1,128}")
        ? value : UUID.randomUUID().toString().replace("-", "");
  }

  public static String current() { return MDC.get(KEY); }

  public static Map<String, String> snapshot() {
    Map<String, String> copy = MDC.getCopyOfContextMap();
    if (copy == null) copy = new HashMap<>();
    copy.put(KEY, resolve(copy.get(KEY)));
    return copy;
  }

  public static void restore(Map<String, String> context) {
    MDC.clear();
    if (context != null && !context.isEmpty()) MDC.setContextMap(context);
  }

  public static Runnable wrap(Runnable task) {
    Map<String, String> captured = snapshot();
    return () -> {
      Map<String, String> previous = MDC.getCopyOfContextMap();
      try { restore(captured); task.run(); }
      finally { restore(previous); }
    };
  }

  public static <T> Callable<T> wrapCallable(Callable<T> task) {
    Map<String, String> captured = snapshot();
    return () -> {
      Map<String, String> previous = MDC.getCopyOfContextMap();
      try { restore(captured); return task.call(); }
      finally { restore(previous); }
    };
  }

  /** Use once per independent scheduled execution, or with a message header per delivery. */
  public static Scope open(String incomingId) { return new Scope(incomingId); }

  public static final class Scope implements AutoCloseable {
    private final Map<String, String> previous = MDC.getCopyOfContextMap();
    private Scope(String incomingId) {
      MDC.clear();
      MDC.put(KEY, resolve(incomingId));
    }
    @Override public void close() { restore(previous); }
  }
}
