package com.festora.subscription.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Records that a specific reminder was already sent to a user, so the scheduler does not
 * re-send the same reminder on subsequent runs.
 *
 * <p>The compound index on {@code (userId, reminderType)} guarantees one row per
 * (user, reminder type) pair, which is the de-duplication key.</p>
 */
@Document(collection = "subscription_reminder_logs")
@CompoundIndex(name = "user_reminder_idx", def = "{'userId': 1, 'reminderType': 1}", unique = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionReminderLog {

    @Id
    private String id;

    private String userId;

    /** e.g. {@code EXPIRY_7D}, {@code EXPIRY_3D}, {@code EXPIRY_1D}, {@code EXPIRED}. */
    private String reminderType;

    private LocalDateTime sentAt;
}
