package com.zeus.springboot.web.http;

import java.net.URI;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Zeus 默认 HTTP 调用工具，适合发送简单 GET 请求和 JSON POST 请求。
 */
public class ZeusHttpClient {

    private final RestClient restClient;

    public ZeusHttpClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * 发送 GET 请求，并以字符串形式返回响应体。
     */
    public String get(String url) {
        return get(URI.create(url), String.class);
    }

    /**
     * 发送 GET 请求，并将响应体转换为指定类型。
     */
    public <T> T get(String url, Class<T> responseType) {
        return get(URI.create(url), responseType);
    }

    /**
     * 发送 GET 请求，并支持带泛型的响应类型。
     */
    public <T> T get(String url, ParameterizedTypeReference<T> responseType) {
        return get(URI.create(url), responseType);
    }

    /**
     * 发送 GET 请求，并以字符串形式返回响应体。
     */
    public String get(URI uri) {
        return get(uri, String.class);
    }

    /**
     * 发送 GET 请求，并将响应体转换为指定类型。
     */
    public <T> T get(URI uri, Class<T> responseType) {
        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(responseType);
    }

    /**
     * 发送 GET 请求，并支持带泛型的响应类型。
     */
    public <T> T get(URI uri, ParameterizedTypeReference<T> responseType) {
        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(responseType);
    }

    /**
     * 发送 JSON POST 请求，并以字符串形式返回响应体。
     */
    public String postJson(String url, Object body) {
        return postJson(URI.create(url), body, String.class);
    }

    /**
     * 发送 JSON POST 请求，并将响应体转换为指定类型。
     */
    public <T> T postJson(String url, Object body, Class<T> responseType) {
        return postJson(URI.create(url), body, responseType);
    }

    /**
     * 发送 JSON POST 请求，并支持带泛型的响应类型。
     */
    public <T> T postJson(String url, Object body, ParameterizedTypeReference<T> responseType) {
        return postJson(URI.create(url), body, responseType);
    }

    /**
     * 发送 JSON POST 请求，并以字符串形式返回响应体。
     */
    public String postJson(URI uri, Object body) {
        return postJson(uri, body, String.class);
    }

    /**
     * 发送 JSON POST 请求，并将响应体转换为指定类型。
     */
    public <T> T postJson(URI uri, Object body, Class<T> responseType) {
        return restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(responseType);
    }

    /**
     * 发送 JSON POST 请求，并支持带泛型的响应类型。
     */
    public <T> T postJson(URI uri, Object body, ParameterizedTypeReference<T> responseType) {
        return restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(responseType);
    }

    /**
     * 暴露底层 RestClient，便于业务代码按需使用更完整的 Spring HTTP API。
     */
    public RestClient restClient() {
        return restClient;
    }
}
