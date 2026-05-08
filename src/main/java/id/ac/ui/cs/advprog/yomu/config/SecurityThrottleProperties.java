package id.ac.ui.cs.advprog.yomu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security.throttle")
public class SecurityThrottleProperties {

    private int loginFailedMaxAttempts = 5;
    private long loginFailedWindowSeconds = 300;
    private int registerMaxAttempts = 5;
    private long registerWindowSeconds = 300;

    public int getLoginFailedMaxAttempts() {
        return loginFailedMaxAttempts;
    }

    public void setLoginFailedMaxAttempts(int loginFailedMaxAttempts) {
        this.loginFailedMaxAttempts = loginFailedMaxAttempts;
    }

    public long getLoginFailedWindowSeconds() {
        return loginFailedWindowSeconds;
    }

    public void setLoginFailedWindowSeconds(long loginFailedWindowSeconds) {
        this.loginFailedWindowSeconds = loginFailedWindowSeconds;
    }

    public int getRegisterMaxAttempts() {
        return registerMaxAttempts;
    }

    public void setRegisterMaxAttempts(int registerMaxAttempts) {
        this.registerMaxAttempts = registerMaxAttempts;
    }

    public long getRegisterWindowSeconds() {
        return registerWindowSeconds;
    }

    public void setRegisterWindowSeconds(long registerWindowSeconds) {
        this.registerWindowSeconds = registerWindowSeconds;
    }
}

