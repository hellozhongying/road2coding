package com.zeus.springboot.web.log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LogValueSanitizerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final LogValueSanitizer sanitizer = new LogValueSanitizer(objectMapper, "***");

    @Test
    void preservesJsonNodeContent() throws Exception {
        JsonNode node = objectMapper.readTree("""
                {
                  "username": "zeus",
                  "role": "admin"
                }
                """);

        Object sanitized = sanitizer.sanitize(node);

        assertThat(sanitized).isInstanceOf(JsonNode.class);
        assertThat(objectMapper.writeValueAsString(sanitized))
                .isEqualTo("{\"username\":\"zeus\",\"role\":\"admin\"}");
    }

    @Test
    void preservesJsonNodeInsideCollections() throws Exception {
        JsonNode node = objectMapper.readTree("""
                {
                  "username": "zeus"
                }
                """);

        Object sanitized = sanitizer.sanitize(List.of(node));

        assertThat(objectMapper.writeValueAsString(sanitized))
                .isEqualTo("[{\"username\":\"zeus\"}]");
    }
}
