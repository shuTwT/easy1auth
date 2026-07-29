package com.easy1auth.foundation.id;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UuidV7Test {
    @Test void generatesRfc9562VersionAndVariant() {
        var id = UuidV7.randomUuid();
        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
    }
}
