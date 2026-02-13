package io.github.supernoobchallenge.nasserver.global.security;

import io.github.supernoobchallenge.nasserver.user.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<Long> {
    private static final Long SYSTEM_USER_ID = 1L;

    @Override

    /**
     * TODO : auditor연동 구현
     */
    public Optional<Long> getCurrentAuditor() {
        return getAuthenticatedAuditor().or(() -> Optional.of(SYSTEM_USER_ID));
    }

    public Optional<Long> getAuthenticatedAuditor() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            HttpSession session = servletAttributes.getRequest().getSession(false);
            if (session == null) {
                return Optional.empty();
            }
            Object userId = session.getAttribute(AuthService.SESSION_USER_ID);
            if (userId instanceof Number number) {
                return Optional.of(number.longValue());
            }
            return Optional.empty();
        }
        return Optional.empty();
    }
}
