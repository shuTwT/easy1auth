package com.easy1auth.framework.common.error;

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

}
