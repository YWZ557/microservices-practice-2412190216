package com.zjgsu.zyw.inertceptor;

import com.zjgsu.zyw.constant.UserConstant;
import com.zjgsu.zyw.context.BaseContext;
import com.zjgsu.zyw.enity.User;
import com.zjgsu.zyw.exception.BaseException;
import com.zjgsu.zyw.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Handle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Objects;

@Slf4j
@Component
public class JwtTokenReaderInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        try {
            String token = request.getHeader("token");
            Integer userId = Integer.valueOf(jwtUtil.parseToken(token).getSubject());
            log.info("userId: {}", userId);
            BaseContext.setCurrentId(userId);
            if (!Objects.equals(jwtUtil.getRole(token), UserConstant.ACCOUNT_TYPE_READER)) {
                throw new BaseException("账号类型错误");
            }
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            return false;
        }


    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        BaseContext.remove();
    }
}
