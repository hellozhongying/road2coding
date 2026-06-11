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

    /**
     * 防重复提交配置。
     */
    private final NoRepeatSubmit noRepeatSubmit = new NoRepeatSubmit();

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

    public NoRepeatSubmit getNoRepeatSubmit() {
        return noRepeatSubmit;
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

    public static class NoRepeatSubmit {

        /**
         * 是否启用 {@code @NoRepeatSubmit} 防重复提交能力。
         */
        private boolean enabled = true;

        /**
         * Redis key 前缀。
         */
        private String keyPrefix = "zeus:web:no-repeat-submit";

        /**
         * 默认防重复提交窗口，单位秒。
         */
        private long interval = 5;

        /**
         * 重复提交时的默认提示信息。
         */
        private String message = "请勿重复提交";

        /**
         * 是否默认把请求参数纳入防重 key。
         */
        private boolean includeParams = true;

        /**
         * 请求头中的用户标识名称；存在该请求头时优先按用户维度防重。
         */
        private String userIdentifyHeader = "X-User-Id";

        /**
         * 未取到用户标识时，是否把客户端 IP 纳入 key。
         */
        private boolean includeClientIp = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public long getInterval() {
            return interval;
        }

        public void setInterval(long interval) {
            this.interval = interval;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public boolean isIncludeParams() {
            return includeParams;
        }

        public void setIncludeParams(boolean includeParams) {
            this.includeParams = includeParams;
        }

        public String getUserIdentifyHeader() {
            return userIdentifyHeader;
        }

        public void setUserIdentifyHeader(String userIdentifyHeader) {
            this.userIdentifyHeader = userIdentifyHeader;
        }

        public boolean isIncludeClientIp() {
            return includeClientIp;
        }

        public void setIncludeClientIp(boolean includeClientIp) {
            this.includeClientIp = includeClientIp;
        }
    }
}
