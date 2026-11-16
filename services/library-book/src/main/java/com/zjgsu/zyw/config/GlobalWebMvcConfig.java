package com.zjgsu.zyw.config;

import com.zjgsu.zyw.inertceptor.JwtTokenAdminInterceptor;
import com.zjgsu.zyw.inertceptor.JwtTokenReaderInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Slf4j
@ConditionalOnClass(WebMvcConfigurer.class)
public class GlobalWebMvcConfig  implements WebMvcConfigurer {

    @Autowired
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;
    @Autowired
    private JwtTokenReaderInterceptor jwtTokenReaderInterceptor;

    public void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");

        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/user/admin/**","/borrow/admin/**","/book/admin/**")
                .excludePathPatterns("/admin/login")
                .excludePathPatterns("/admin/loginout");

        registry.addInterceptor(jwtTokenReaderInterceptor)
                .addPathPatterns("/user/reader/**", "/borrow/reader/**","/book/reader/**")
                .excludePathPatterns("/reader/login")
                .excludePathPatterns("/reader/loginout");
    }

}
