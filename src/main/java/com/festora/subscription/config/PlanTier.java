package com.festora.subscription.config;

/**
 * Logical subscription tiers used for feature gating and marketing.
 *
 * <p>A user's tier is resolved from the {@code subscriptionPlan} string stored on the
 * {@code User} document (e.g. {@code premium_yearly} -> {@link #PREMIUM}). The special
 * {@link #TRIAL} tier is granted at signup and carries the full {@link #PREMIUM} feature set.</p>
 */
public enum PlanTier {

    /** Free / lapsed tier. Limited feature set. */
    FREE,

    /** Entry-level paid tier. */
    BASIC,

    /** Top paid tier. Full feature set. */
    PREMIUM,

    /** 1-month free trial granted at signup. Treated as {@link #PREMIUM} for features. */
    TRIAL
}
