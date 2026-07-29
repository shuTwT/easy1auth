package com.easy1auth.security;
import org.junit.jupiter.api.Test; import static org.assertj.core.api.Assertions.*;
class SecurityDataCipherTest {
 @Test void bindsCiphertextToAssociatedData(){var cipher=new SecurityDataCipher("abcdefghijklmnopqrstuvwxyz012345");String encrypted=cipher.encrypt("tenant:a","secret");assertThat(cipher.decrypt("tenant:a",encrypted)).isEqualTo("secret");assertThatThrownBy(()->cipher.decrypt("tenant:b",encrypted)).isInstanceOf(IllegalStateException.class);}
 @Test void rejectsShortMasterKey(){assertThatThrownBy(()->new SecurityDataCipher("short")).isInstanceOf(IllegalStateException.class);}
}
