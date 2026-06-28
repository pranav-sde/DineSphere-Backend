package com.festora.subscription.dto;

import com.festora.subscription.config.PlanFeatures;
import com.festora.subscription.config.PlanTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for the authenticated endpoint ({@code GET /subscription/me}).
 *
 * <p>Tells the admin frontend which tier the user is on, what features they have,
 * when their subscription expires, and whether it is currently active.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MySubscriptionResponse {

    /** The effective tier (accounts for expiry — expired plans show as FREE). */
    private PlanTier tier;

    /** Display name for the tier, e.g. "Premium". */
    private String tierDisplayName;

    /** The raw plan string stored on the user (e.g. "premium_yearly", "TRIAL"). */
    private String subscriptionPlan;

    /** Whether the subscription is currently active (not expired). */
    private boolean active;

    /** When the subscription expires (null if never subscribed). */
    private LocalDateTime subscriptionExpiry;

    /** The full feature set for the user's effective tier. */
    private PlanFeatures features;
}
