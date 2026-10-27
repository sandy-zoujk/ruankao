package com.sandy.ruankao.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 跨域与登录拦截配置。
 *
 * @author zoujunke
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
  private final AuthInterceptor authInterceptor;
  private final AdminInterceptor adminInterceptor;

  public WebConfig(AuthInterceptor authInterceptor, AdminInterceptor adminInterceptor) {
    this.authInterceptor = authInterceptor;
    this.adminInterceptor = adminInterceptor;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    // 开发环境允许本机任意端口的 Vue 前端访问后端接口。
    registry
        .addMapping("/**")
        .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .exposedHeaders("X-Trace-Id")
        .allowCredentials(true);
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    // 登录、注册接口必须公开，其余业务接口统一校验登录令牌。
    registry
        .addInterceptor(authInterceptor)
        .addPathPatterns("/**")
        .excludePathPatterns("/auth/login", "/auth/register", "/software/info", "/error");
    registry.addInterceptor(adminInterceptor).addPathPatterns("/admin/**");
  }
}
