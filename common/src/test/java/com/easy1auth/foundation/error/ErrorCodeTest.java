package com.easy1auth.foundation.error;

import com.easy1auth.common.foundation.error.DomainException;
import com.easy1auth.common.foundation.error.ErrorCode;
import com.easy1auth.common.foundation.web.ApiResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorCodeTest {
    @Test
    void rejectsInvalidDefinitions() {
        assertThrows(IllegalArgumentException.class, () -> new ErrorCode(0, "invalid"));
        assertThrows(IllegalArgumentException.class, () -> new ErrorCode(10000, " "));
    }

    @Test
    void domainExceptionExposesStableErrorCodeAndMessage() {
        var errorCode = new ErrorCode(12345, "业务失败");
        var exception = new DomainException(errorCode);

        assertSame(errorCode, exception.errorCode());
        assertEquals(12345, exception.code());
        assertEquals("业务失败", exception.getMessage());
    }

    @Test
    void apiResponseUsesBusinessCode() {
        var response = ApiResponse.error(new ErrorCode(12345, "业务失败"));

        assertEquals(12345, response.code());
        assertNull(response.data());
        assertEquals("业务失败", response.msg());
    }
}
