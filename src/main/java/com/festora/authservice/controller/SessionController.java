package com.festora.authservice.controller;

import com.festora.authservice.dto.SessionStartRequest;
import com.festora.authservice.dto.SessionStartResponse;
import com.festora.authservice.service.CustomerSessionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/auth/session")
@RequiredArgsConstructor
public class SessionController {

    private final CustomerSessionService sessionService;
    @PostMapping("/start")
    public ResponseEntity<SessionStartResponse> start(
            @RequestBody SessionStartRequest request,
            HttpServletRequest req) {
        try {
            return ResponseEntity.ok(sessionService.startSession(request, req));
        } catch (Exception e) {
            System.out.println("Session start failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<SessionStartResponse> refresh(@RequestBody com.festora.authservice.dto.SessionRefreshRequest request) {
        try {
            return ResponseEntity.ok(sessionService.refreshSession(request.getRefreshToken()));
        } catch (Exception e) {
            System.out.println("Session refresh failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}

