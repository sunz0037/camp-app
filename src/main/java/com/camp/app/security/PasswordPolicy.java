package com.camp.app.security;

import com.camp.app.api.BizException;
import com.camp.app.config.AuthSecurityProperties;
import com.camp.app.util.PasswordEncoderUtil;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class PasswordPolicy {
    private static final String UPPERS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWERS = "abcdefghijkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SPECIALS = "!@#$%^*_-";

    private final AuthSecurityProperties.Password rules;
    private final SecureRandom random = new SecureRandom();

    public PasswordPolicy(AuthSecurityProperties properties) {
        this.rules = properties.getPassword();
    }

    public void check(String raw, String username, String currentEncoded, List<String> olderEncoded) {
        if (raw == null || raw.length() < Math.max(rules.getMinLength(), 1)) {
            throw BizException.unprocessable("密码长度不能少于" + rules.getMinLength() + "位");
        }
        if (rules.isRequireUpper() && !raw.chars().anyMatch(ch -> ch >= 'A' && ch <= 'Z')) {
            throw BizException.unprocessable("密码须包含大写字母");
        }
        if (rules.isRequireLower() && !raw.chars().anyMatch(ch -> ch >= 'a' && ch <= 'z')) {
            throw BizException.unprocessable("密码须包含小写字母");
        }
        if (rules.isRequireDigit() && !raw.chars().anyMatch(Character::isDigit)) {
            throw BizException.unprocessable("密码须包含数字");
        }
        if (rules.isRequireSpecial() && raw.chars().allMatch(Character::isLetterOrDigit)) {
            throw BizException.unprocessable("密码须包含特殊字符");
        }
        if (rules.isRejectUsername() && username != null && raw.equalsIgnoreCase(username.trim())) {
            throw BizException.unprocessable("密码不能与用户名相同");
        }
        if (matches(raw, currentEncoded)) {
            throw BizException.unprocessable("新密码不能与当前密码相同");
        }
        if (olderEncoded != null) {
            for (String encoded : olderEncoded) {
                if (matches(raw, encoded)) {
                    throw BizException.unprocessable("新密码不能与最近使用过的密码相同");
                }
            }
        }
    }

    public String generate() {
        List<Character> chars = new ArrayList<>();
        if (rules.isRequireUpper()) {
            chars.add(pick(UPPERS));
        }
        if (rules.isRequireLower()) {
            chars.add(pick(LOWERS));
        }
        if (rules.isRequireDigit()) {
            chars.add(pick(DIGITS));
        }
        if (rules.isRequireSpecial()) {
            chars.add(pick(SPECIALS));
        }
        String alphabet = UPPERS + LOWERS + DIGITS + (rules.isRequireSpecial() ? SPECIALS : "");
        int length = Math.max(rules.getMinLength(), 12);
        while (chars.size() < length) {
            chars.add(pick(alphabet));
        }
        Collections.shuffle(chars, random);
        StringBuilder builder = new StringBuilder(chars.size());
        for (Character ch : chars) {
            builder.append(ch);
        }
        return builder.toString();
    }

    private char pick(String alphabet) {
        return alphabet.charAt(random.nextInt(alphabet.length()));
    }

    private boolean matches(String raw, String encoded) {
        if (raw == null || encoded == null || encoded.isBlank()) {
            return false;
        }
        try {
            return PasswordEncoderUtil.matches(raw, encoded);
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
