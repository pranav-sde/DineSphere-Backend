package com.festora.authservice.controller;

import com.festora.authservice.dto.AuthResponse;
import com.festora.authservice.dto.LoginRequest;
import com.festora.authservice.dto.RefreshRequest;
import com.festora.authservice.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/health")
    public String health() {
        return "Auth Service OK !!!";
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest req) {
        return authService.login(req.getEmail(), req.getPassword());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody RefreshRequest req) {
        return authService.refresh(req.getRefreshToken());
    }

    @PostMapping("/logout")
    public void logout(@RequestBody RefreshRequest req) {
        authService.logout(req.getRefreshToken());
    }
}