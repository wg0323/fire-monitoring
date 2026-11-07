package com.fm.common.exception;

import com.fm.module.workorder.dto.WorkOrderFinishDTO;
import com.fm.module.workorder.dto.WorkOrderQueryDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 全局异常处理测试。
 */
class GlobalExceptionHandlerTest {

    @Test
    void should_returnBadRequest_when_queryParamTypeMismatch() throws Exception {
        mockMvc().perform(get("/test/workorder/page").param("status", "undefined"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请求参数不合法"));
    }

    @Test
    void should_returnFieldMessage_when_requestBodyInvalid() throws Exception {
        mockMvc().perform(post("/test/workorder/finish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remark\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("result 处理结论不能为空"));
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new QueryTestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @RestController
    static class QueryTestController {

        @GetMapping("/test/workorder/page")
        public String page(WorkOrderQueryDTO query) {
            return "ok";
        }

        @PostMapping("/test/workorder/finish")
        public String finish(@Valid @RequestBody WorkOrderFinishDTO request) {
            return "ok";
        }
    }
}
