package com.masterhesse.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * 内部服务认证过滤器
 * 用于保护 /internal/** 接口，验证 X-Internal-Token 请求头
 */
@Slf4j
@Component
public class InternalTokenFilter extends OncePerRequestFilter {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    @Value("${security.internal.token:}")
    private String configuredInternalToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestPath = request.getServletPath();

        // 只处理 /internal/** 路径
        if (requestPath.startsWith("/internal/")) {
            String token = request.getHeader(INTERNAL_TOKEN_HEADER);

            if (StringUtils.hasText(token) && StringUtils.hasText(configuredInternalToken)) {
                if (token.equals(configuredInternalToken)) {
                    // Token 验证通过，设置系统服务身份
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    "INTERNAL_SERVICE",
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_INTERNAL"))
                            );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Internal service authenticated for path: {}", requestPath);
                } else {
                    log.warn("Invalid internal token from: {}", request.getRemoteAddr());
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
