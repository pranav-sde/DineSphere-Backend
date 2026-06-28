package com.festora.subscription.controller;

import com.festora.authservice.model.User;
import com.festora.authservice.repository.UserRepository;
import com.festora.paymentservice.config.SubscriptionPlanConfig;
import com.festora.subscription.config.PlanFeatureConfig;
import com.festora.subscription.config.PlanFeatures;
import com.festora.subscription.config.PlanTier;
import com.festora.subscription.dto.MySubscriptionResponse;
import com.festora.subscription.dto.PlanMarketingResponse;
import com.festora.subscription.service.SubscriptionFeatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Subscription plan and feature endpoints.
 *
 * <ul>
 *   <li>{@code GET /subscription/plans} — Public. Returns pricing + features + marketing copy
 *       for all tiers. Used by the admin frontend pricing page.</li>
 *   <li>{@code GET /subscription/me} — Authenticated. Returns the current user's effective
 *       tier, feature set, expiry date and active status.</li>
 * </ul>
 */
@RestController
@RequestMapping("/subscription")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionPlanController {

    private final SubscriptionPlanConfig subscriptionPlanConfig;
    private final PlanFeatureConfig planFeatureConfig;
    private final SubscriptionFeatureService featureService;
    private final UserRepository userRepository;

    /**
     * Public marketing endpoint — returns the full pricing catalogue.
     * No authentication required (added to SecurityConfig permitAll).
     */
    @GetMapping("/plans")
    public ResponseEntity<PlanMarketingResponse> getPlans() {
        List<PlanMarketingResponse.TierDetail> tiers = new ArrayList<>();

        for (PlanTier tier : List.of(PlanTier.FREE, PlanTier.BASIC, PlanTier.PREMIUM)) {
            PlanFeatures features = featureService.getFeaturesForTier(tier);
            PlanFeatureConfig.Marketing marketing = planFeatureConfig.getMarketing() != null
                    ? planFeatureConfig.getMarketing().get(tier) : null;

            List<PlanMarketingResponse.BillingOption> pricing = buildPricing(tier);

            tiers.add(PlanMarketingResponse.TierDetail.builder()
                    .tier(tier)
                    .displayName(marketing != null ? marketing.getDisplayName() : tier.name())
                    .tagline(marketing != null ? marketing.getTagline() : null)
                    .highlights(marketing != null ? marketing.getHighlights() : List.of())
                    .popular(marketing != null && marketing.isPopular())
                    .pricing(pricing)
                    .features(features)
                    .build());
        }

        return ResponseEntity.ok(PlanMarketingResponse.builder().tiers(tiers).build());
    }

    /**
     * Authenticated endpoint — returns the current user's subscription status and features.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMySubscription(
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        PlanTier effectiveTier = featureService.resolveEffectiveTier(user);
        PlanFeatures features = featureService.getFeaturesForTier(effectiveTier);
        boolean isActive = !featureService.isExpired(user);

        return ResponseEntity.ok(MySubscriptionResponse.builder()
                .tier(effectiveTier)
                .tierDisplayName(featureService.getDisplayName(effectiveTier))
                .subscriptionPlan(user.getSubscriptionPlan())
                .active(isActive)
                .subscriptionExpiry(user.getSubscriptionExpiry())
                .features(features)
                .build());
    }

    /**
     * Build billing options for a tier from the plan config.
     * For FREE tier, returns a single zero-price option.
     */
    private List<PlanMarketingResponse.BillingOption> buildPricing(PlanTier tier) {
        List<PlanMarketingResponse.BillingOption> options = new ArrayList<>();

        if (tier == PlanTier.FREE) {
            options.add(PlanMarketingResponse.BillingOption.builder()
                    .planId("free")
                    .price(0)
                    .months(0)
                    .label("Free")
                    .effectiveMonthlyPrice(0.0)
                    .build());
            return options;
        }

        Map<String, SubscriptionPlanConfig.PlanDetails> plans = subscriptionPlanConfig.getPlans();
        if (plans == null) {
            return options;
        }

        String prefix = tier.name().toLowerCase();
        for (Map.Entry<String, SubscriptionPlanConfig.PlanDetails> entry : plans.entrySet()) {
            String planId = entry.getKey();
            if (!planId.startsWith(prefix)) {
                continue;
            }
            SubscriptionPlanConfig.PlanDetails details = entry.getValue();
            String label = details.getMonths() == 1 ? "Monthly" : "Yearly";
            Double effectiveMonthly = details.getMonths() > 1
                    ? Math.round((details.getPrice() / details.getMonths()) * 100.0) / 100.0
                    : null;

            options.add(PlanMarketingResponse.BillingOption.builder()
                    .planId(planId)
                    .price(details.getPrice())
                    .months(details.getMonths())
                    .label(label)
                    .effectiveMonthlyPrice(effectiveMonthly)
                    .build());
        }

        return options;
    }
}
