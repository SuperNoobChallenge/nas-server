package io.github.supernoobchallenge.nasserver.user.controller;

import io.github.supernoobchallenge.nasserver.global.security.AuditorAwareImpl;
import io.github.supernoobchallenge.nasserver.user.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class WebAuthPageController {
    private final AuthService authService;
    private final AuditorAwareImpl auditorAware;

    @GetMapping("/")
    public String home() {
        return auditorAware.getAuthenticatedAuditor().isPresent()
                ? "redirect:/web/directories"
                : "redirect:/web/login";
    }

    @GetMapping("/web/login")
    public String loginPage() {
        if (auditorAware.getAuthenticatedAuditor().isPresent()) {
            return "redirect:/web/directories";
        }
        return "web/login";
    }

    @PostMapping("/web/login")
    public String login(
            @RequestParam String loginId,
            @RequestParam String password,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        try {
            authService.login(loginId, password, request);
            return "redirect:/web/directories";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/web/login";
        }
    }

    @PostMapping("/web/logout")
    public String logout(HttpServletRequest request) {
        authService.logout(request);
        return "redirect:/web/login";
    }
}
