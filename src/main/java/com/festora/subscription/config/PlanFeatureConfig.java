package com.festora.subscription.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;

/**
 * Binds the {@code app.subscription.features.*} block from {@code application.yml}.
 *
 * <p>Two sub-sections:</p>
 * <ul>
 *   <li>{@code tiers} - a per-tier {@link PlanFeatures} map, keyed by {@link PlanTier} name.</li>
 *   <li>{@code marketing} - display-only metadata (name, tagline, highlighted bullets,
 *       "popular" badge) used by the public pricing endpoint.</li>
 * </ul>
 */
@Configuration
@ConfigurationProperties(prefix = "app.subscription.features")
@Data
public class PlanFeatureConfig {

    private Map<PlanTier, PlanFeatures> tiers = new EnumMap<>(PlanTier.class);

    private Map<PlanTier, Marketing> marketing = new EnumMap<>(PlanTier.class);

    /**
     * Display / marketing metadata for a tier. Returned as-is to the frontend pricing page.
     */
    @Data
    public static class Marketing {
        /** Human-readable plan name, e.g. "Basic". */
        private String displayName;
        /** One-line sales tagline. */
        private String tagline;
        /** Short list of headline features to show as bullets. */
        private java.util.List<String> highlights = new java.util.ArrayList<>();
        /** Whether the pricing card should be badged "Most Popular". */
        private boolean popular;
    }
}
