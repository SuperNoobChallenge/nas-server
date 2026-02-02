package io.github.supernoobchallenge.nasserver.global.security;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<Long> {

    @Override

    /**
     * TODO : auditor연동 구현
     */
    public Optional<Long> getCurrentAuditor() {
        // 인증된 사용자의 식별자(PK)를 꺼내서 반환해야 한다.
        return Optional.of(0L);
    }
}
