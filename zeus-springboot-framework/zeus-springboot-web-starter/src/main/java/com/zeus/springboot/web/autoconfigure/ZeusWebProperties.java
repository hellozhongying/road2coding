package com.zeus.springboot.web.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "zeus.web")
public class ZeusWebProperties {

    /**
     * Zeus Web Starter 全局开关。
     */
    private boolean enabled = true;

    /**
     * 统一响应包装配置。
     */
    private final ResponseWrap responseWrap = new ResponseWrap();

    /**
     * API 访问日志配置。
     */
    private final ApiLog apiLog = new ApiLog();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ResponseWrap getResponseWrap() {
        return responseWrap;
    }

    public ApiLog getApiLog() {
        return apiLog;
    }

    public static class ResponseWrap {

        /**
         * 不进行统一包装的请求路径，支持 Ant 风格匹配表达式。
         */
        private List<String> excludePaths = new ArrayList<>();

        public List<String> getExcludePaths() {
            return excludePaths;
        }

        public void setExcludePaths(List<String> excludePaths) {
            this.excludePaths = excludePaths == null ? new ArrayList<>() : excludePaths;
        }
    }

    public static class ApiLog {

        /**
         * 单条 API 日志中，请求参数或响应结果序列化后的最大长度。
         */
        private int maxLength = 1000;

        /**
         * {@code @LogMask} 标记字段在日志中的替换文本。
         */
        private String maskText = "***";

        public int getMaxLength() {
            return maxLength;
        }

        public void setMaxLength(int maxLength) {
            this.maxLength = maxLength;
        }

        public String getMaskText() {
            return maskText;
        }

        public void setMaskText(String maskText) {
            this.maskText = maskText;
        }
    }
}
