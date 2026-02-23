package io.github.supernoobchallenge.nasserver.global.bootstrap;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "system.account")
public class SystemAccountProperties {
    private String loginId = "system";
    private String password = "system";
    private String email = "system@nasserver.local";

    public String resolveEmail() {
        if (email != null && !email.isBlank()) {
            return email;
        }
        return loginId + "@nasserver.local";
    }
}
