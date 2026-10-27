package com.sandy.ruankao.logging;

import java.io.IOException;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class TraceIdHttpInterceptor implements ClientHttpRequestInterceptor {
  @Override public ClientHttpResponse intercept(HttpRequest request, byte[] body,
      ClientHttpRequestExecution execution) throws IOException {
    String id = TraceContext.current();
    if (id != null) {
      request.getHeaders().set(TraceContext.HEADER, id);
      return execution.execute(request, body);
    }
    try (TraceContext.Scope ignored = TraceContext.open(null)) {
      request.getHeaders().set(TraceContext.HEADER, TraceContext.current());
      return execution.execute(request, body);
    }
  }
}
