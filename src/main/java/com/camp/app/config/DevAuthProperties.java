package com.camp.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * profile=h2、profile=dev，或 camp.dev-auth.enabled=true 时接受开发态身份。
 */
@Component
public class DevAuthProperties {
    private final boolean enabledFlag;
    private final Environment environment;

    public DevAuthProperties(
            @Value("${camp.dev-auth.enabled:false}") boolean enabledFlag,
            Environment environment) {
        this.enabledFlag = enabledFlag;
        this.environment = environment;
    }

    public boolean isEnabled() {
        return enabledFlag || environment.acceptsProfiles(Profiles.of("h2", "dev"));
    }
}
