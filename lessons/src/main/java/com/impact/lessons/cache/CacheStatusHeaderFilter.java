package com.impact.lessons.cache;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CacheStatusHeaderFilter extends OncePerRequestFilter {
    public static final String HEADER_NAME = "X-Impact-Cache";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            CacheRequestContext.Status status = CacheRequestContext.getStatus();
            if (status != null) {
                response.setHeader(HEADER_NAME, status.name());
            }
            CacheRequestContext.clear();
        }
    }
}

