package com.zjgsu.zyw.config;

import com.zjgsu.zyw.context.BaseContext;
import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DefaultFeignConfig {
    @Bean
    public Logger.Level fullFeignLoggerLevel(){
        return Logger.Level.FULL;
    }
    @Bean
    public RequestInterceptor requestInterceptor(){
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                Integer userInfo = BaseContext.getCurrentId();
                if(userInfo != null){
                    template.header("user-info", userInfo.toString());
                }

            }
        };
    }
}
