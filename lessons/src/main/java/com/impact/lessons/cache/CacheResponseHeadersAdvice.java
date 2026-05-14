package com.impact.lessons.cache;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Pune header-ele de cache înainte ca body-ul JSON să fie scris pe response.
 * Un {@link jakarta.servlet.Filter} în {@code finally} poate rula prea târziu (response deja committed),
 * de aceea Postman nu vedea {@code X-Impact-Cache} / {@code X-Impact-Cache-Backend}.
 */
@ControllerAdvice
public class CacheResponseHeadersAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        CacheRequestContext.Status status = CacheRequestContext.getStatus();
        if (status != null) {
            response.getHeaders().set(CacheStatusHeaderFilter.HEADER_NAME, status.name());
        }
        String backend = CacheRequestContext.getBackend();
        if (backend != null) {
            response.getHeaders().set(CacheStatusHeaderFilter.HEADER_BACKEND_NAME, backend);
        }
        return body;
    }
}
