package com.paradissaveurs.controller;

import com.paradissaveurs.dto.SessionInfoDto;
import com.paradissaveurs.service.AuthService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/session")
    @PreAuthorize("isAuthenticated()")
    public SessionInfoDto session(Authentication auth) {
        return authService.sessionInfo(auth);
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public void logout(Authentication auth) {
        authService.logout(auth);
    }
}
