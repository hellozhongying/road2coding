package com.zeus.springboot.web.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeus.springboot.web.annotation.IgnoreResponseWrap;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class ResponseWrapAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    public ResponseWrapAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return !returnType.hasMethodAnnotation(IgnoreResponseWrap.class)
                && !returnType.getContainingClass().isAnnotationPresent(IgnoreResponseWrap.class);
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (body instanceof Result<?>) {
            return body;
        }

        Result<Object> result = Result.success(body, RequestIdHolder.currentRequestId());
        if (StringHttpMessageConverter.class.isAssignableFrom(selectedConverterType)) {
            try {
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return objectMapper.writeValueAsString(result);
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Failed to serialize response result.", exception);
            }
        }
        return result;
    }
}
