package com.sandy.ruankao.logging;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Retains diagnostic frames and exception types without logging input-bearing messages. */
public final class LogExceptions {
  private LogExceptions() {}

  public static Throwable withoutMessages(Throwable failure) {
    return copy(failure, Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>()));
  }

  private static Throwable copy(Throwable failure, Set<Throwable> visited) {
    if (failure == null || !visited.add(failure)) return null;
    Throwable safe = new RuntimeException(failure.getClass().getName());
    safe.setStackTrace(failure.getStackTrace());
    Throwable cause = copy(failure.getCause(), visited);
    if (cause != null) safe.initCause(cause);
    for (Throwable suppressed : failure.getSuppressed()) {
      Throwable item = copy(suppressed, visited);
      if (item != null) safe.addSuppressed(item);
    }
    return safe;
  }
}
