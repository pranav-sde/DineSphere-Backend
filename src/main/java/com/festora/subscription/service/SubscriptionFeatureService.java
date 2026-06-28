package com.festora.subscription.service;

import com.festora.subscription.config.PlanFeatureConfig;
import com.festora.subscription.config.PlanFeatures;
import com.festora.subscription.config.PlanTier;
import com.festora.authservice.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Resolves a user's {@link PlanTier} and the {@link PlanFeatures} they are entitled to.
 *
 * <p>Tier resolution rules:</p>
 * <ol>
 *   <li>If the subscription has expired ({@code subscriptionExpiry} in the past) the effective
 *       tier is {@link PlanTier#FREE}, regardless of the stored plan string.</li>
 *   <li>Otherwise the stored {@code subscriptionPlan} string is mapped to a tier:
 *       {@code basic_*} -> {@link PlanTier#BASIC}, {@code premium_*} -> {@link PlanTier#PREMIUM},
 *       {@code TRIAL} -> {@link PlanTier#TRIAL}. Unknown values fall back to {@link PlanTier#FREE}.</li>
 * </ol>
 *
 * <p>The returned {@link PlanFeatures} always carries every field populated — if a tier is missing
 * from config, the field defaults defined on {@link PlanFeatures} apply, so callers never see
 * nulls.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionFeatureService {

    private final PlanFeatureConfig planFeatureConfig;

    /**
     * Resolve the raw tier from the stored plan string, ignoring expiry.
     */
    public PlanTier resolveTier(String subscriptionPlan) {
        if (subscriptionPlan == null || subscriptionPlan.isBlank()) {
            return PlanTier.FREE;
        }
        String normalized = subscriptionPlan.trim().toUpperCase();
        if (normalized.equals(PlanTier.TRIAL.name())) {
            return PlanTier.TRIAL;
        }
        if (normalized.startsWith("PREMIUM")) {
            return PlanTier.PREMIUM;
        }
        if (normalized.startsWith("BASIC")) {
            return PlanTier.BASIC;
        }
        // Unknown plan strings (e.g. legacy "1_MONTHS") degrade gracefully to FREE.
        return PlanTier.FREE;
    }

    /**
     * Resolve the <em>effective</em> tier for a user, factoring in expiry.
     * Expired subscriptions always resolve to {@link PlanTier#FREE}.
     */
    public PlanTier resolveEffectiveTier(User user) {
        if (user == null) {
            return PlanTier.FREE;
        }
        if (isExpired(user)) {
            return PlanTier.FREE;
        }
        return resolveTier(user.getSubscriptionPlan());
    }

    /**
     * Resolve the <em>effective</em> tier for a user, factoring in expiry.
     */
    public PlanTier resolveEffectiveTier(String subscriptionPlan, LocalDateTime expiry) {
        if (expiry == null || expiry.isBefore(LocalDateTime.now())) {
            return PlanTier.FREE;
        }
        return resolveTier(subscriptionPlan);
    }

    public boolean isExpired(User user) {
        return user.getSubscriptionExpiry() == null
                || user.getSubscriptionExpiry().isBefore(LocalDateTime.now());
    }

    /**
     * Get the full feature set for an <em>effective</em> tier. Always non-null and fully populated.
     */
    public PlanFeatures getFeaturesForTier(PlanTier tier) {
        return Optional.ofNullable(planFeatureConfig.getTiers())
                .map(m -> m.get(tier))
                .orElseGet(PlanFeatures::new);
    }

    /**
     * Get the full feature set for a user, honouring expiry.
     */
    public PlanFeatures getFeatures(User user) {
        return getFeaturesForTier(resolveEffectiveTier(user));
    }

    /**
     * Convenience check: does the user's plan grant a given boolean feature?
     */
    public boolean hasFeature(User user, Feature feature) {
        return feature.isEnabled(getFeatures(user));
    }

    /**
     * Resolve a human-readable label for the stored plan (e.g. for display). Falls back to
     * the tier name when no marketing display name is configured.
     */
    public String getDisplayName(PlanTier tier) {
        return Optional.ofNullable(planFeatureConfig.getMarketing())
                .map(m -> m.get(tier))
                .map(PlanFeatureConfig.Marketing::getDisplayName)
                .orElse(tier.name());
    }

    /**
     * Boolean feature keys understood by {@link #hasFeature(User, Feature)}.
     */
    public enum Feature {
        WHATSAPP_NOTIFICATIONS,
        TELEGRAM_NOTIFICATIONS,
        INVENTORY_MANAGEMENT,
        ORDER_LOGS,
        CUSTOM_BRANDING,
        MULTIPLE_BRANCHES,
        PRIORITY_SUPPORT;

        public boolean isEnabled(PlanFeatures f) {
            return switch (this) {
                case WHATSAPP_NOTIFICATIONS -> f.isWhatsappNotifications();
                case TELEGRAM_NOTIFICATIONS -> f.isTelegramNotifications();
                case INVENTORY_MANAGEMENT -> f.isInventoryManagement();
                case ORDER_LOGS -> f.isOrderLogs();
                case CUSTOM_BRANDING -> f.isCustomBranding();
                case MULTIPLE_BRANCHES -> f.isMultipleBranches();
                case PRIORITY_SUPPORT -> f.isPrioritySupport();
            };
        }
    }
}
