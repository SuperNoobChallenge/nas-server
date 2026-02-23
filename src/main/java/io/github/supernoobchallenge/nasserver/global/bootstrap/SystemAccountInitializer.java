package io.github.supernoobchallenge.nasserver.global.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SystemAccountInitializer implements ApplicationRunner {
    private final SystemAccountProvisioningService provisioningService;
    private final SystemAccountProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        if (properties.getLoginId() == null || properties.getLoginId().isBlank()) {
            log.warn("system.account.login-id가 비어 있어 시스템 계정 동기화를 건너뜁니다.");
            return;
        }
        if (properties.getPassword() == null || properties.getPassword().isBlank()) {
            log.warn("system.account.password가 비어 있어 시스템 계정 동기화를 건너뜁니다.");
            return;
        }

        provisioningService.provisionOrSync(properties);
    }
}
