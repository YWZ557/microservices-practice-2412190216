package com.zjgsu.zyw.filter;


import com.zjgsu.zyw.config.AuthProperties;
import com.zjgsu.zyw.utils.JwtTool;
import lombok.RequiredArgsConstructor;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final AuthProperties authProperties;
    private final JwtTool jwtTool;
    private final AntPathMatcher antPathMatcher=new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 获取request
        ServerHttpRequest request = exchange.getRequest();

        // 2. 判断是否需要做登录拦截
        if (isExclude(request.getPath().toString())) {
            return chain.filter(exchange);
        }

        // 3. 获取token
        String auth = request.getHeaders().getFirst("Authorization");
        // 4. 校验并解析token

        String token = null;
        if (auth != null && auth.startsWith("Bearer ")) {
            token = auth.substring(7);
        }
        Integer userId;
        try {
            userId = Integer.valueOf(jwtTool.parseToken(token).getSubject());
            // TODO 5. 传递用户信息
            System.out.println("userid " + userId);

        } catch (Exception e) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        String userInfo = userId.toString();
        ServerWebExchange serverWebExchange = exchange.mutate()
                .request(builder -> builder.header("user-info", userInfo))
                .build();
        // 6. 放行
        return chain.filter(serverWebExchange);
    }

    private boolean isExclude(String string) {
        for(String path:authProperties.getExcludePaths()){
            if (antPathMatcher.match(path,string)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
