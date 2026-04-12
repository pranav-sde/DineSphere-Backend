package com.festora.authservice.service;

import com.festora.authservice.dto.CreateOwnerRequest;
import com.festora.authservice.dto.UserResponse;
import com.festora.authservice.enums.UserRole;
import com.festora.authservice.model.User;
import com.festora.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse createRestaurantOwner(CreateOwnerRequest req) {

        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .email(req.getEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(UserRole.OWNER)
                .restaurantId(req.getRestaurantId())
                .active(true)
                .build();

        User saved = userRepository.save(user);

        return new UserResponse(
                saved.getId(),
                saved.getEmail(),
                saved.getRole(),
                saved.getRestaurantId(),
                saved.isActive()
        );
    }
}