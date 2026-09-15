package com.easy1auth.framework.web.response;

import com.easy1auth.framework.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiResponseTest {
    @Test
    void usesBusinessErrorCode() {
        var response = ApiResponse.error(new ErrorCode(12345, "业务失败"));

        assertEquals(12345, response.code());
        assertNull(response.data());
        assertEquals("业务失败", response.msg());
    }
}
