package io.github.supernoobchallenge.nasserver;

import io.github.supernoobchallenge.nasserver.config.SecurityConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(SecurityConfig.class)
public class NasServerApplication {

	static void main(String[] args) {
		SpringApplication.run(NasServerApplication.class, args);
	}
}
