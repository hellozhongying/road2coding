package com.zeus.springboot.web.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeus.springboot.web.annotation.IgnoreResponseWrap;
import com.zeus.springboot.web.autoconfigure.ZeusWebProperties;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.List;

/**
 * Controller 响应体统一包装处理器。
 *
 * <p>除显式忽略或配置排除的接口外，会把原始返回值包装为 {@link Result}，形成稳定的前后端响应契约。</p>
 */
@RestControllerAdvice
public class ResponseWrapAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    private final List<String> excludePaths;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public ResponseWrapAdvice(ObjectMapper objectMapper) {
        this(objectMapper, new ZeusWebProperties());
    }

    public ResponseWrapAdvice(ObjectMapper objectMapper, ZeusWebProperties properties) {
        this.objectMapper = objectMapper;
        this.excludePaths = properties.getResponseWrap().getExcludePaths();
    }

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        // 类或方法上标记 @IgnoreResponseWrap 时，完全跳过统一响应包装。
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
        if (isExcludedPath(request)) {
            return body;
        }
        if (body instanceof Result<?>) {
            return body;
        }

        Result<Object> result = Result.success(body);
        if (StringHttpMessageConverter.class.isAssignableFrom(selectedConverterType)) {
            try {
                // StringHttpMessageConverter 只能写字符串，这里手动转 JSON 并修正响应类型。
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return objectMapper.writeValueAsString(result);
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Failed to serialize response result.", exception);
            }
        }
        return result;
    }

    private boolean isExcludedPath(ServerHttpRequest request) {
        if (CollectionUtils.isEmpty(excludePaths)) {
            return false;
        }
        String path = request.getURI().getPath();
        return excludePaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
}
