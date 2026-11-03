package com.camp.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 密码复杂度与登录锁定。规则放在配置里，改密失败时返回明确业务码。
 */
@Data
@Component
@ConfigurationProperties(prefix = "camp.auth")
public class AuthSecurityProperties {
    private Password password = new Password();
    private Lock lock = new Lock();

    @Data
    public static class Password {
        private int minLength = 8;
        private boolean requireUpper = true;
        private boolean requireLower = true;
        private boolean requireDigit = true;
        private boolean requireSpecial = true;
        private boolean rejectUsername = true;
        /**
         * 除当前密码外再拒绝的历史口令条数。0 表示只拒绝与当前密码相同。
         * 历史口令暂存在进程内，重启后只保留「不能与当前密码相同」这一条。
         */
        private int historySize = 0;
    }

    @Data
    public static class Lock {
        /** 小于等于 0 时关闭锁定。 */
        private int maxFailures = 5;
        private int lockSeconds = 900;
    }
}
