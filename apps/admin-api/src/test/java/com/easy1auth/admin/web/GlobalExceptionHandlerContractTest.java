package com.easy1auth.admin.web;

import com.easy1auth.foundation.error.DomainException;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerContractTest {
    @Test
    void domainErrorUsesStandardEnvelope() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mvc.perform(get("/failure"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10000))
                .andExpect(jsonPath("$.msg").value("请求失败"))
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    @RestController
    static class FailingController {
        @GetMapping("/failure")
        void fail() {
            throw new DomainException("TEST_FAILURE", "请求失败", 400);
        }
    }
}
