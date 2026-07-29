package com.easy1auth.security;
import org.junit.jupiter.api.Test; import java.time.Instant; import static org.assertj.core.api.Assertions.*;
class TotpServiceTest {
 private final TotpService totp=new TotpService();
 @Test void acceptsAdjacentWindowAndRejectsReplay(){String secret=totp.secret();Instant now=Instant.ofEpochSecond(1_800_000_000L);long step=totp.step(now);String previous=totp.code(secret,step-1);assertThat(totp.verify(secret,previous,now,null)).isTrue();assertThat(totp.verify(secret,previous,now,step-1)).isFalse();}
 @Test void rejectsMalformedCode(){assertThat(totp.verify(totp.secret(),"12345x",Instant.now(),null)).isFalse();}
}
