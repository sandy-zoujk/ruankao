package com.sandy.ruankao;

import com.sandy.ruankao.logging.TraceContext;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:trace_web;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TraceWebIntegrationTest {
  @Autowired MockMvc mvc;

  @Test void publicAndRejectedRequestsExposeTraceHeader() throws Exception {
    mvc.perform(get("/software/info").header("X-Trace-Id", "web-123")
        .header("Origin", "http://localhost:5173"))
        .andExpect(status().isOk()).andExpect(header().string("X-Trace-Id", "web-123"))
        .andExpect(header().string("Access-Control-Expose-Headers", "X-Trace-Id"));
    assertNull(MDC.getCopyOfContextMap());
    mvc.perform(get("/admin/users").header("X-Trace-Id", "denied-123"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("X-Trace-Id", "denied-123"));
    assertNull(MDC.getCopyOfContextMap());
  }

  @Test void mvcCallablePreservesTraceAfterRequestThreadReturns() throws Exception {
    MvcResult result = mvc.perform(get("/software/info").param("traceTest", "true")
        .header("X-Trace-Id", "async-123"))
        .andExpect(request().asyncStarted()).andReturn();
    assertNull(TraceContext.current());
    mvc.perform(asyncDispatch(result)).andExpect(status().isOk())
        .andExpect(content().string("async-123"))
        .andExpect(header().string("X-Trace-Id", "async-123"));
    assertNull(TraceContext.current());
  }

  @TestConfiguration
  static class Config {
    @Bean AsyncController asyncController() { return new AsyncController(); }
  }
  @RestController
  static class AsyncController {
    @GetMapping(value = "/software/info", params = "traceTest=true")
    public Callable<String> call() { return TraceContext::current; }
  }
}
