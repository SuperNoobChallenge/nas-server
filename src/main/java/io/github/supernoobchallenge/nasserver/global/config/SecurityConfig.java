package io.github.supernoobchallenge.nasserver.global.config;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http){
        http
                // 1. CSRF 보안 끄기
                // (켜져 있으면 기존의 POST, PUT, DELETE 요청이 토큰 없음으로 403 에러가 발생함)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. 모든 요청에 대해 인증(로그인) 없이 접근 허용
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )

                // 3. 기본 로그인 폼이나 Basic Auth 끄기 (선택 사항)
                // (이 설정이 없으면 브라우저가 자동으로 시큐리티 기본 로그인 창을 띄울 수 있음)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

}
