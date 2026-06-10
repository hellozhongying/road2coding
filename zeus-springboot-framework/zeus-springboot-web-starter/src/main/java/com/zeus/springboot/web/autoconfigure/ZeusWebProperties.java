package com.zeus.springboot.web.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "zeus.web")
public class ZeusWebProperties {

    /**
     * Global switch for Zeus web starter features.
     */
    private boolean enabled = true;

    /**
     * Response wrapping options.
     */
    private final ResponseWrap responseWrap = new ResponseWrap();

    /**
     * API log options.
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
         * Ant-style request paths that should keep their original response body.
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
         * Maximum length of a serialized request parameter or response result in one API log.
         */
        private int maxLength = 1000;

        /**
         * Replacement text used for fields annotated with {@code @LogMask}.
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
