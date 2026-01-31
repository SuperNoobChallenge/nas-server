package io.github.supernoobchallenge.nasserver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "io.github.supernoobchallenge.nasserver.repository")
public class JpaConfig {
}
