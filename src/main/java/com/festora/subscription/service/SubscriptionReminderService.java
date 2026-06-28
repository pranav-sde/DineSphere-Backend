package com.festora.subscription.service;

import com.festora.authservice.model.User;
import com.festora.authservice.repository.UserRepository;
import com.festora.authservice.service.EmailService;
import com.festora.notificationservice.service.NotificationDispatcher;
import com.festora.subscription.model.SubscriptionReminderLog;
import com.festora.subscription.repository.SubscriptionReminderLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Sends subscription expiry reminders (7 / 3 / 1 days before, plus a post-expiry notice)
 * and performs hard deactivation of lapsed accounts.
 *
 * <p>Each reminder type is recorded in {@link SubscriptionReminderLog} so a user never gets the
 * same reminder twice (idempotent across scheduler runs and restarts).</p>
 *
 * <p>Channels: Email is always attempted (every owner has an email). WhatsApp / Telegram are
 * dispatched via {@link NotificationDispatcher} only when the restaurant has that integration
 * configured.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionReminderService {

    /** Reminder schedule: how many days before expiry to nudge. */
    private static final int[] REMINDER_DAYS_BEFORE = {7, 3, 1};

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final NotificationDispatcher notificationDispatcher;
    private final SubscriptionReminderLogRepository reminderLogRepository;

    /**
     * Scan for users in the configured pre-expiry windows and send reminders.
     */
    public void sendExpiryReminders() {
        for (int daysBefore : REMINDER_DAYS_BEFORE) {
            LocalDateTime windowStart = LocalDate.now(ZoneId.systemDefault())
                    .plusDays(daysBefore).atStartOfDay();
            LocalDateTime windowEnd = windowStart.plusDays(1);

            List<User> due = userRepository.findBySubscriptionExpiryBetween(windowStart, windowEnd);
            if (due.isEmpty()) {
                continue;
            }
            log.info("Sending {}d expiry reminders to {} user(s)", daysBefore, due.size());

            String reminderType = "EXPIRY_" + daysBefore + "D";
            for (User user : due) {
                if (alreadySent(user.getId(), reminderType)) {
                    continue;
                }
                sendReminder(user, daysBefore, reminderType);
                markSent(user.getId(), reminderType);
            }
        }
    }

    /**
     * Send the post-expiry notice to users whose subscription has just lapsed (today).
     */
    public void sendExpiredNotices() {
        // Users who expired in the last 3 days. A 3-day window (not 1 day) tolerates the
        // scheduler skipping a day or two (server downtime, deploys) so the expired notice
        // is never silently dropped. De-dup via the reminder log prevents re-sending.
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusDays(3);
        List<User> recentlyLapsed = userRepository.findBySubscriptionExpiryBetween(cutoff, now);

        for (User user : recentlyLapsed) {
            if (alreadySent(user.getId(), "EXPIRED")) {
                continue;
            }
            sendExpiredNotice(user);
            markSent(user.getId(), "EXPIRED");
        }
    }

    /**
     * Hard-deactivate any account whose subscription has lapsed but is still active=true.
     * Per product decision: no grace period — expired accounts are deactivated immediately.
     *
     * @return number of accounts deactivated in this run
     */
    public int deactivateExpiredUsers() {
        LocalDateTime now = LocalDateTime.now();
        List<User> lapsed = userRepository.findBySubscriptionExpiryBeforeAndActiveTrue(now);
        if (lapsed.isEmpty()) {
            return 0;
        }
        log.info("Hard-deactivating {} lapsed subscription(s)", lapsed.size());
        for (User user : lapsed) {
            user.setActive(false);
            userRepository.save(user);
        }
        return lapsed.size();
    }

    private void sendReminder(User user, int daysLeft, String reminderType) {
        String email = user.getEmail();
        String restaurant = user.getRestaurantName();

        // 1. Email (always available)
        if (email != null && !email.isBlank()) {
            emailService.sendSubscriptionExpiryReminder(email, restaurant, daysLeft, user.getSubscriptionExpiry());
        }

        // 2. WhatsApp + Telegram (only if integration configured)
        if (user.getRestaurantId() != null) {
            String pushMsg = buildPushMessage(user, daysLeft, false);
            notificationDispatcher.dispatch(user.getRestaurantId(), pushMsg);
        }
    }

    private void sendExpiredNotice(User user) {
        String email = user.getEmail();
        String restaurant = user.getRestaurantName();

        if (email != null && !email.isBlank()) {
            emailService.sendSubscriptionExpiredNotice(email, restaurant, user.getSubscriptionExpiry());
        }
        if (user.getRestaurantId() != null) {
            String pushMsg = buildPushMessage(user, 0, true);
            notificationDispatcher.dispatch(user.getRestaurantId(), pushMsg);
        }
    }

    private String buildPushMessage(User user, int daysLeft, boolean expired) {
        String name = user.getRestaurantName() != null ? user.getRestaurantName() : "there";
        if (expired) {
            return String.format(
                    "⚠️ *DineSphere Subscription Expired*\n\n" +
                    "Hi %s, your subscription has expired and your account has been moved to the Free plan.\n" +
                    "Renew now to restore QR ordering, notifications and analytics: " +
                    "https://admin.dinesphere.co/billing",
                    name);
        }
        return String.format(
                "🔔 *DineSphere Subscription Reminder*\n\n" +
                "Hi %s, your subscription expires in %d day(s).\n" +
                "Renew now to avoid losing access: https://admin.dinesphere.co/billing",
                name, daysLeft);
    }

    private boolean alreadySent(String userId, String reminderType) {
        return reminderLogRepository.existsByUserIdAndReminderType(userId, reminderType);
    }

    private void markSent(String userId, String reminderType) {
        reminderLogRepository.save(SubscriptionReminderLog.builder()
                .userId(userId)
                .reminderType(reminderType)
                .sentAt(LocalDateTime.now())
                .build());
    }
}
