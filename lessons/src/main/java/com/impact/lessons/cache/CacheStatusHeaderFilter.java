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
    /** Valoare vizibilă în Postman (tab Headers): ce implementare CacheClient a rulat (memory vs Redis). */
    public static final String HEADER_BACKEND_NAME = "X-Impact-Cache-Backend";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Header-ele se pun în CacheResponseHeadersAdvice (înainte de serializarea body),
            // aici doar curățăm ThreadLocal ca să nu leak-uim între request-uri.
            CacheRequestContext.clear();
        }
    }
}

