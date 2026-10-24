package com.sandy.ruankao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * 软考学习系统后端启动入口。
 *
 * @author zoujunke
 */
@SpringBootApplication
public class RuankaoStudyApplication {
    public static void main(String[] args) {
        SpringApplication.run(RuankaoStudyApplication.class, args);
    }
}
