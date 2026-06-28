package com.festora.subscription.config;

import lombok.Data;

/**
 * The set of feature flags + quantitative limits attached to a single {@link PlanTier}.
 *
 * <p>Values are loaded from {@code app.subscription.features.tiers} in {@code application.yml}.
 * Any value not supplied there falls back to the default declared on the field, so the system
 * always has a complete feature set even with a partial config.</p>
 *
 * <p>Convention for numeric limits: {@code -1} means <em>unlimited</em>.</p>
 */
@Data
public class PlanFeatures {

    /** Max menu items a restaurant can publish. {@code -1} = unlimited. */
    private int maxMenuItems = -1;

    /** Max tables / QR codes that can be mapped. {@code -1} = unlimited. */
    private int maxTables = -1;

    /** Max menu categories. {@code -1} = unlimited. */
    private int maxCategories = -1;

    /** Whether WhatsApp order notifications are available. */
    private boolean whatsappNotifications = true;

    /** Whether Telegram order notifications are available. */
    private boolean telegramNotifications = true;

    /** Whether inventory / stock management is available. */
    private boolean inventoryManagement = true;

    /** Whether order logs / audit trail are available. */
    private boolean orderLogs = true;

    /** Whether custom branding (logo, theme) is allowed. */
    private boolean customBranding = true;

    /** Whether multi-branch / multi-outlet is supported. */
    private boolean multipleBranches = true;

    /** Analytics depth: {@code BASIC}, {@code STANDARD} or {@code ADVANCED}. */
    private String analyticsLevel = "STANDARD";

    /** Whether priority support is included. */
    private boolean prioritySupport = true;
}
