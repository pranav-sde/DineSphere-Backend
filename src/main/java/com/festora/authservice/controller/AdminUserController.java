package com.festora.authservice.controller;

import com.festora.authservice.dto.CreateOwnerRequest;
import com.festora.authservice.dto.UserResponse;
import com.festora.authservice.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @PostMapping("/owners")
//    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse createOwner(@Valid @RequestBody CreateOwnerRequest request) {
        return adminUserService.createRestaurantOwner(request);
    }
}
