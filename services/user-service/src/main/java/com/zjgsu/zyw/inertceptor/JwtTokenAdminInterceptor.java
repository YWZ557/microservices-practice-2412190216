package com.zjgsu.zyw.inertceptor;

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
import org.springframework.web.servlet.ModelAndView;

import java.util.Objects;

@Component
@Slf4j
public class JwtTokenAdminInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(!(handler instanceof HandlerMethod)){
            return true;
        }

        String token = request.getHeader("token");
        try{
            log.info("开始解析token");
            Integer userId = Integer.valueOf(jwtUtil.parseToken(token).getSubject());
            BaseContext.setCurrentId(userId);
            log.info("userId = {}",userId);
            String type = jwtUtil.getRole(token);
            if(!Objects.equals(type, UserConstant.ACCOUNT_TYPE_ADMIN)){
                throw new BaseException("账号类型错误");
            }
            return true;

        }catch (Exception e){
            response.setStatus(401);
            return false;
        }

    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        BaseContext.remove();
    }
}
