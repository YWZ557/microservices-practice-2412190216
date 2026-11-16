package com.zjgsu.zyw.filter;


import com.zjgsu.zyw.config.AuthProperties;
import com.zjgsu.zyw.utils.JwtTool;
import io.jsonwebtoken.Claims;
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

        ServerHttpRequest request = exchange.getRequest();

        if (isExclude(request.getPath().toString())) {
            return chain.filter(exchange);
        }

        String token = request.getHeaders().getFirst("Authorization");

        Integer userId;
        String accountType;
        try {
            Claims claims = jwtTool.parseToken(token);
            userId = Integer.valueOf(claims.getSubject());
            accountType = claims.get("role", String.class);

        } catch (Exception e) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }


        if (request.getPath().toString().contains("admin") && !"ADMIN".equals(accountType)) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return response.setComplete();
        }
        if (request.getPath().toString().contains("reader") && !"READER".equals(accountType)) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return response.setComplete();
        }

        String userInfo = userId.toString();
        ServerWebExchange serverWebExchange = exchange.mutate()
                .request(builder -> builder.header("user-info", userInfo))
                .build();

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
