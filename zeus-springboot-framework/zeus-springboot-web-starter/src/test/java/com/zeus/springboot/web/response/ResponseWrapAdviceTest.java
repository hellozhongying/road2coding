package com.zeus.springboot.web.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeus.springboot.web.annotation.IgnoreResponseWrap;
import com.zeus.springboot.web.exception.CommonErrorCode;
import com.zeus.springboot.web.exception.GlobalExceptionHandler;
import com.zeus.springboot.web.exception.ParamException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResponseWrapAdviceTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new ResponseWrapAdvice(new ObjectMapper().findAndRegisterModules()),
                    new GlobalExceptionHandler())
            .build();

    @Test
    void wrapsObjectResponse() throws Exception {
        mockMvc.perform(get("/object").header(RequestIdHolder.REQUEST_ID_HEADER, "request-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.name").value("zeus"))
                .andExpect(jsonPath("$.requestId").value("request-1"))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void wrapsStringResponse() throws Exception {
        mockMvc.perform(get("/string"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data").value("zeus"));
    }

    @Test
    void skipsWrappingWhenAnnotated() throws Exception {
        mockMvc.perform(get("/raw"))
                .andExpect(status().isOk())
                .andExpect(content().string("raw"));
    }

    @Test
    void exceptionResponseIsResult() throws Exception {
        mockMvc.perform(get("/param-error"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("参数错误"))
                .andExpect(jsonPath("$.data", nullValue()))
                .andExpect(jsonPath("$.requestId", notNullValue()))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @RestController
    static class TestController {

        @GetMapping("/object")
        Map<String, String> object() {
            return Map.of("name", "zeus");
        }

        @GetMapping("/string")
        String string() {
            return "zeus";
        }

        @IgnoreResponseWrap
        @GetMapping("/raw")
        String raw() {
            return "raw";
        }

        @GetMapping("/param-error")
        void paramError() {
            throw new ParamException(CommonErrorCode.PARAM_ERROR);
        }
    }
}
