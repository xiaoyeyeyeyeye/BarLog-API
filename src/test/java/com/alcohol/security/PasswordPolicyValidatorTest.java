package com.alcohol.security;

import com.alcohol.common.BizException;
import com.alcohol.config.SecurityProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyValidatorTest {

    @Test
    void rejectsWeakPassword() {
        SecurityProperties props = new SecurityProperties();
        PasswordPolicyValidator validator = new PasswordPolicyValidator(props);
        assertThrows(BizException.class, () -> validator.validate("1234567"));
        assertThrows(BizException.class, () -> validator.validate("abcdefgh"));
        assertDoesNotThrow(() -> validator.validate("password1"));
    }
}
