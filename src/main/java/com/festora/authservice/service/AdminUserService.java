package com.festora.authservice.service;

import com.festora.authservice.dto.CreateOwnerRequest;
import com.festora.authservice.dto.UserResponse;
import com.festora.authservice.dto.event.SignupNotificationEvent;
import com.festora.authservice.enums.UserRole;
import com.festora.authservice.model.User;
import com.festora.authservice.repository.UserRepository;
import com.festora.paymentservice.config.SubscriptionPlanConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final SubscriptionPlanConfig subscriptionPlanConfig;

    public UserResponse createRestaurantOwner(CreateOwnerRequest req) {

        if (userRepository.existsByEmail(req.getBusinessEmail())) {
            throw new com.festora.authservice.exception.DuplicateResourceException("Email already exists");
        }

        if (userRepository.existsByPhoneNumber(req.getPhoneNumber())) {
            throw new com.festora.authservice.exception.DuplicateResourceException("Phone number already exists");
        }

        Long nextRestaurantId = userRepository.findTopByOrderByRestaurantIdDesc()
                .map(u -> u.getRestaurantId() + 1)
                .orElse(1000L);

        User user = User.builder()
                .email(req.getBusinessEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(UserRole.OWNER)
                .restaurantId(nextRestaurantId)
                .ownerName(req.getOwnerName())
                .phoneNumber(req.getPhoneNumber())
                .fssaiLicense(req.getFssaiLicense())
                .restaurantName(req.getRestaurantName())
                .address(req.getAddress())
                .gstNumber(req.getGstNumber())
                .enableDelivery(req.isEnableDelivery())
                .deliveryRadius(req.getDeliveryRadius())
                .minOrderValue(req.getMinOrderValue())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .active(true)
                .subscriptionPlan("TRIAL")
                .subscriptionExpiry(java.time.LocalDateTime.now().plusDays(15))
                .build();

        User saved = userRepository.save(user);

        // Notify admin about new signup via WhatsApp & Telegram
        eventPublisher.publishEvent(new SignupNotificationEvent(this, saved));

        return new UserResponse(
                saved.getId(),
                saved.getEmail(),
                saved.getRole(),
                saved.getRestaurantId(),
                saved.isActive()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void renewSubscription(String userId, int months) {
        renewSubscription(userId, months, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void renewSubscription(String userId, int months, String planName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate plan ID against known plans
        if (planName != null && !planName.isBlank()) {
            String normalizedPlan = planName.trim().toUpperCase();
            if (normalizedPlan.equals("TRIAL")) {
                throw new IllegalArgumentException("TRIAL plan cannot be renewed or switched to — it is a one-time signup offer");
            }
            boolean planExists = subscriptionPlanConfig.getPlans() != null
                    && subscriptionPlanConfig.getPlans().containsKey(planName.trim().toLowerCase());
            if (!planExists) {
                throw new IllegalArgumentException("Unknown subscription plan: " + planName);
            }
        }

        // New plan always starts fresh from now — no proration credit from previous plan.
        // This means switching plans forfeits any remaining days on the old plan.
        java.time.LocalDateTime newExpiry = java.time.LocalDateTime.now().plusMonths(months);
        user.setActive(true);
        user.setSubscriptionExpiry(newExpiry);
        user.setSubscriptionPlan(planName != null ? planName.toUpperCase() : months + "_MONTHS");
        userRepository.save(user);
    }

    public com.festora.authservice.dto.OwnerProfileResponse getProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return com.festora.authservice.dto.OwnerProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .ownerName(user.getOwnerName())
                .phoneNumber(user.getPhoneNumber())
                .restaurantName(user.getRestaurantName())
                .address(user.getAddress())
                .fssaiLicense(user.getFssaiLicense())
                .gstNumber(user.getGstNumber())
                .enableDelivery(user.isEnableDelivery())
                .deliveryRadius(user.getDeliveryRadius())
                .minOrderValue(user.getMinOrderValue())
                .latitude(user.getLatitude())
                .longitude(user.getLongitude())
                .restaurantId(user.getRestaurantId())
                .build();
    }

    public com.festora.authservice.dto.OwnerProfileResponse updateProfile(String userId, com.festora.authservice.dto.UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setOwnerName(req.getOwnerName());
        user.setPhoneNumber(req.getPhoneNumber());
        user.setRestaurantName(req.getRestaurantName());
        user.setAddress(req.getAddress());
        user.setFssaiLicense(req.getFssaiLicense());
        user.setGstNumber(req.getGstNumber());
        user.setEnableDelivery(req.isEnableDelivery());
        user.setDeliveryRadius(req.getDeliveryRadius());
        user.setMinOrderValue(req.getMinOrderValue());
        user.setLatitude(req.getLatitude());
        user.setLongitude(req.getLongitude());

        User saved = userRepository.save(user);

        return com.festora.authservice.dto.OwnerProfileResponse.builder()
                .id(saved.getId())
                .email(saved.getEmail())
                .ownerName(saved.getOwnerName())
                .phoneNumber(saved.getPhoneNumber())
                .restaurantName(saved.getRestaurantName())
                .address(saved.getAddress())
                .fssaiLicense(saved.getFssaiLicense())
                .gstNumber(saved.getGstNumber())
                .enableDelivery(saved.isEnableDelivery())
                .deliveryRadius(saved.getDeliveryRadius())
                .minOrderValue(saved.getMinOrderValue())
                .latitude(saved.getLatitude())
                .longitude(saved.getLongitude())
                .restaurantId(saved.getRestaurantId())
                .build();
    }

    public boolean existsByEmail(String email) {
        if (email == null) return false;
        return userRepository.existsByEmail(email.toLowerCase());
    }

    public void resetPassword(String email, String newPassword) {
        if (email == null || newPassword == null) {
            throw new RuntimeException("Email and password are required");
        }
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}