package com.festora.subscription.dto;

import com.festora.subscription.config.PlanFeatures;
import com.festora.subscription.config.PlanTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for the public pricing endpoint ({@code GET /subscription/plans}).
 *
 * <p>Merges pricing (from {@code app.subscription.plans}) with feature gates and marketing
 * metadata so the admin frontend can render a complete pricing page in a single call.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanMarketingResponse {

    /** All available tiers with their pricing, features and marketing copy. */
    private List<TierDetail> tiers;

    /**
     * A single tier's full representation for the pricing page.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TierDetail {

        /** The logical tier key (FREE, BASIC, PREMIUM). TRIAL is excluded from pricing. */
        private PlanTier tier;

        /** Human-readable name, e.g. "Basic". */
        private String displayName;

        /** One-line sales tagline. */
        private String tagline;

        /** Headline feature bullets for the pricing card. */
        private List<String> highlights;

        /** Whether the card should be badged "Most Popular". */
        private boolean popular;

        /** Available billing options keyed by plan id (e.g. "basic_monthly" -> price info). */
        private List<BillingOption> pricing;

        /** The feature set that comes with this tier. */
        private PlanFeatures features;
    }

    /**
     * A single billing option for a tier (monthly, yearly, etc.).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillingOption {

        /** Plan id matching the key in {@code app.subscription.plans} (e.g. "basic_monthly"). */
        private String planId;

        /** Price in INR. */
        private double price;

        /** Subscription duration in months. */
        private int months;

        /** Human-readable label, e.g. "Monthly" or "Yearly". */
        private String label;

        /** Effective monthly price when paying yearly (null for monthly plans). */
        private Double effectiveMonthlyPrice;
    }
}
