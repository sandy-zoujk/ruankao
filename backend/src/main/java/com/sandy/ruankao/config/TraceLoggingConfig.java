package com.sandy.ruankao.config;

import com.sandy.ruankao.logging.*;
import java.util.concurrent.ThreadPoolExecutor;
import javax.servlet.DispatcherType;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableAsync
public class TraceLoggingConfig implements WebMvcConfigurer {
  @Bean
  public FilterRegistrationBean<TraceIdFilter> traceIdFilter() {
    FilterRegistrationBean<TraceIdFilter> bean = new FilterRegistrationBean<>(new TraceIdFilter());
    bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
    bean.setAsyncSupported(true);
    bean.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
    bean.addUrlPatterns("/*");
    return bean;
  }

  @Bean(name = {"taskExecutor", "applicationTaskExecutor"})
  public ThreadPoolTaskExecutor traceTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(16);
    executor.setQueueCapacity(200);
    executor.setThreadNamePrefix("trace-task-");
    executor.setTaskDecorator(new MdcTaskDecorator());
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(30);
    return executor;
  }

  @Override public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
    configurer.setTaskExecutor(traceTaskExecutor());
  }

  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder.additionalInterceptors(new TraceIdHttpInterceptor())
        .setConnectTimeout(java.time.Duration.ofSeconds(5))
        .setReadTimeout(java.time.Duration.ofSeconds(10)).build();
  }
}
