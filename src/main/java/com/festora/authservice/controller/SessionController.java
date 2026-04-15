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

@RestController
@RequestMapping("/auth/session")
@RequiredArgsConstructor
public class SessionController {

    private final CustomerSessionService sessionService;
    @PostMapping("/start")
    public SessionStartResponse start(
            @RequestBody SessionStartRequest request,
            HttpServletRequest req) {
        try {
            return sessionService.startSession(request, req);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
}

