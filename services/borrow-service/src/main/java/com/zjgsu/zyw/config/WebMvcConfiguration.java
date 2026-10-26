package com.zjgsu.zyw.config;

import com.zjgsu.zyw.inertceptor.JwtTokenAdminInterceptor;
import com.zjgsu.zyw.inertceptor.JwtTokenReaderInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

@Configuration
@Slf4j
public class WebMvcConfiguration extends WebMvcConfigurationSupport {

    @Autowired
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;
    @Autowired
    private JwtTokenReaderInterceptor jwtTokenReaderInterceptor;

    protected void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");

        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/login");

        registry.addInterceptor(jwtTokenReaderInterceptor)
                .addPathPatterns("/api/reader/**")
                .excludePathPatterns("/api/reader/login");
    }

}
