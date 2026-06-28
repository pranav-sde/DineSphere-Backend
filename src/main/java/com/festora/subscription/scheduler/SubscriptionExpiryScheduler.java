package com.festora.subscription.scheduler;

import com.festora.subscription.service.SubscriptionReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily subscription lifecycle job. Runs at 09:00 IST (03:30 UTC) every day and:
 * <ol>
 *   <li>Sends 7 / 3 / 1-day pre-expiry reminders (de-duplicated via reminder logs).</li>
 *   <li>Sends the "subscription expired" notice to users who lapsed today.</li>
 *   <li>Hard-deactivates any account whose subscription has passed its expiry date.</li>
 * </ol>
 *
 * <p>Can be disabled by setting {@code app.subscription.scheduler.enabled=false}.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.subscription.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SubscriptionExpiryScheduler {

    private final SubscriptionReminderService reminderService;

    /**
     * 09:00 IST daily. IST = UTC+5:30, so 03:30 UTC.
     */
    @Scheduled(cron = "${app.subscription.scheduler.cron:0 30 3 * * *}", zone = "Asia/Kolkata")
    public void runDailySubscriptionJob() {
        log.info("Subscription daily job started");
        try {
            reminderService.sendExpiryReminders();
            reminderService.sendExpiredNotices();
            int deactivated = reminderService.deactivateExpiredUsers();
            if (deactivated > 0) {
                log.info("Subscription daily job deactivated {} lapsed account(s)", deactivated);
            }
        } catch (Exception e) {
            log.error("Subscription daily job failed", e);
        }
        log.info("Subscription daily job finished");
    }
}
