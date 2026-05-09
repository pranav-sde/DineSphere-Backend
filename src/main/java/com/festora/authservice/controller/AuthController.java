package com.festora.authservice.controller;

import com.festora.authservice.dto.*;
import com.festora.authservice.service.AdminUserService;
import com.festora.authservice.service.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final AdminUserService adminUserService;
    private final OtpService otpService;

    public AuthController(AuthService authService, AdminUserService adminUserService, OtpService otpService) {
        this.authService = authService;
        this.adminUserService = adminUserService;
        this.otpService = otpService;
    }

    @PostMapping("/send-otp")
    public void sendOtp(@RequestBody java.util.Map<String, String> req) {
        String email = req.get("email");
        if (email == null || email.isEmpty()) {
            throw new RuntimeException("Email is required");
        }
        otpService.generateAndSendOtp(email);
    }

    @PostMapping("/verify-otp")
    public org.springframework.http.ResponseEntity<String> verifyOtp(@RequestBody java.util.Map<String, String> req) {
        String email = req.get("email");
        String otp = req.get("otp");
        
        if (otpService.verifyOtp(email, otp)) {
            return org.springframework.http.ResponseEntity.ok("Email verified successfully");
        } else {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST).body("Invalid or expired OTP");
        }
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

    @PostMapping("/signup")
    public UserResponse createOwner(@Valid @RequestBody CreateOwnerRequest request) {
        return adminUserService.createRestaurantOwner(request);
    }

    @PostMapping("/renew/{id}")
    public void renew(@PathVariable String id, @RequestParam int months) {
        adminUserService.renewSubscription(id, months);
    }

    @GetMapping("/profile")
    public com.festora.authservice.dto.OwnerProfileResponse getProfile(jakarta.servlet.http.HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (userId == null) throw new RuntimeException("Unauthorized");
        return adminUserService.getProfile(userId);
    }

    @PutMapping("/profile")
    public com.festora.authservice.dto.OwnerProfileResponse updateProfile(
            jakarta.servlet.http.HttpServletRequest request,
            @RequestBody com.festora.authservice.dto.UpdateProfileRequest body) {
        String userId = request.getHeader("X-User-Id");
        if (userId == null) throw new RuntimeException("Unauthorized");
        return adminUserService.updateProfile(userId, body);
    }
}