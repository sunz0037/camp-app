package com.camp.app.security;

import com.camp.app.api.BizException;
import com.camp.app.config.AuthSecurityProperties;
import com.camp.app.util.PasswordEncoderUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyTest {

    @Test
    void rejectsWeakPasswordsWithSpecificMessages() {
        PasswordPolicy policy = policy();
        assertEquals(422, failure(policy, "abcdef", "admin", null).getCode());
        assertEquals("密码长度不能少于8位", failure(policy, "abcdef", "admin", null).getMessage());
        assertEquals("密码须包含大写字母", failure(policy, "abcdefg1!", "admin", null).getMessage());
        assertEquals("密码须包含小写字母", failure(policy, "ABCDEFG1!", "admin", null).getMessage());
        assertEquals("密码须包含数字", failure(policy, "Abcdefgh!", "admin", null).getMessage());
        assertEquals("密码须包含特殊字符", failure(policy, "Abcdefg1", "admin", null).getMessage());
        assertEquals("密码不能与用户名相同", failure(policy, "Abcdefg1!", "Abcdefg1!", null).getMessage());
    }

    @Test
    void rejectsCurrentAndOlderHashes() {
        PasswordPolicy policy = policy();
        String current = PasswordEncoderUtil.encode("Oldpass@123");
        String older = PasswordEncoderUtil.encode("Older@1234");
        assertEquals("新密码不能与当前密码相同", failure(policy, "Oldpass@123", "admin", current).getMessage());
        BizException prior = assertThrows(BizException.class,
                () -> policy.check("Older@1234", "admin", current, List.of(older)));
        assertEquals("新密码不能与最近使用过的密码相同", prior.getMessage());
        assertDoesNotThrow(() -> policy.check("Newpass@123", "admin", current, List.of(older)));
    }

    @Test
    void generatedPasswordSatisfiesRules() {
        PasswordPolicy policy = policy();
        assertDoesNotThrow(() -> policy.check(policy.generate(), "admin", null, List.of()));
    }

    private static BizException failure(PasswordPolicy policy, String raw, String username, String current) {
        return assertThrows(BizException.class, () -> policy.check(raw, username, current, List.of()));
    }

    private static PasswordPolicy policy() {
        return new PasswordPolicy(new AuthSecurityProperties());
    }
}
