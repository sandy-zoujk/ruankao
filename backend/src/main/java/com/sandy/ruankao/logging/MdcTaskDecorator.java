package com.sandy.ruankao.logging;

import org.springframework.core.task.TaskDecorator;

public class MdcTaskDecorator implements TaskDecorator {
  @Override public Runnable decorate(Runnable runnable) {
    return TraceContext.wrap(runnable);
  }
}
