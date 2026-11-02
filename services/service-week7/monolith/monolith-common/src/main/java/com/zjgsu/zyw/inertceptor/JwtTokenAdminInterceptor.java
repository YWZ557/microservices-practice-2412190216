package com.zjgsu.zyw.inertceptor;

import cn.hutool.core.util.StrUtil;
import com.zjgsu.zyw.constant.UserConstant;
import com.zjgsu.zyw.context.BaseContext;
import com.zjgsu.zyw.exception.BaseException;
import com.zjgsu.zyw.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Objects;

@Component
@Slf4j
public class JwtTokenAdminInterceptor implements HandlerInterceptor {


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(!(handler instanceof HandlerMethod)){
            return true;
        }

        String userInfo = request.getHeader("user-info");
        if(!StrUtil.isBlank(userInfo)){
            BaseContext.setCurrentId(Integer.valueOf(userInfo));
        }
        return true;
    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        BaseContext.remove();
    }
}
