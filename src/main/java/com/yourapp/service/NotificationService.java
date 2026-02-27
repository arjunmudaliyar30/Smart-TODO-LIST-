package com.yourapp.service;

import com.yourapp.model.Notification;
import com.yourapp.model.NotificationChannel;
import com.yourapp.model.User;
import com.yourapp.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final WhatsAppService whatsAppService;
    private final EmailService emailService;
    private final WebPushService webPushService;

    public NotificationService(NotificationRepository notificationRepository,
                               WhatsAppService whatsAppService,
                               EmailService emailService,
                               @Lazy WebPushService webPushService) {
        this.notificationRepository = notificationRepository;
        this.whatsAppService = whatsAppService;
        this.emailService = emailService;
        this.webPushService = webPushService;
    }

    /**
     * Route a notification to the user's preferred channel.
     * If the external channel fails, falls back to IN_APP automatically.
     * Never throws — all failures are logged and swallowed.
     *
     * @param user    the recipient
     * @param message notification text
     * @param type    arbitrary type label e.g. "REMINDER", "TIMEOUT", "SYSTEM"
     */
    public void sendNotification(User user, String message, String type) {
        if (user == null) {
            log.warn("NOTIFICATION_SKIP reason=null_user type={}", type);
            return;
        }

        NotificationChannel preferred = user.getPreferredNotificationChannel();
        if (preferred == null) {
            preferred = NotificationChannel.IN_APP;
        }

        log.info("NOTIFICATION channel={} userId={} type={}", preferred, user.getId(), type);

        switch (preferred) {
            case WHATSAPP -> {
                boolean sent = sendViaWhatsApp(user, message);
                if (!sent) {
                    log.warn("NOTIFICATION_FALLBACK from=WHATSAPP to=IN_APP userId={}", user.getId());
                    saveInApp(user, message, type, NotificationChannel.IN_APP);
                } else {
                    saveInApp(user, message, type, NotificationChannel.WHATSAPP);
                }
            }
            case EMAIL -> {
                boolean sent = sendViaEmail(user, message);
                if (!sent) {
                    log.warn("NOTIFICATION_FALLBACK from=EMAIL to=IN_APP userId={}", user.getId());
                    saveInApp(user, message, type, NotificationChannel.IN_APP);
                } else {
                    saveInApp(user, message, type, NotificationChannel.EMAIL);
                }
            }
            default -> saveInApp(user, message, type, NotificationChannel.IN_APP);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private boolean sendViaWhatsApp(User user, String message) {
        try {
            String phone = user.getPhoneNumber();
            if (phone == null || phone.isBlank()) {
                log.warn("WHATSAPP_SKIP reason=no_phone userId={}", user.getId());
                return false;
            }
            whatsAppService.sendWhatsAppMessage(phone, message);
            return true;
        } catch (Exception e) {
            log.error("WHATSAPP_NOTIFICATION_FAIL userId={} error={}", user.getId(), e.getMessage());
            return false;
        }
    }

    private boolean sendViaEmail(User user, String message) {
        try {
            String email = user.getEmail();
            if (email == null || email.isBlank()) {
                log.warn("EMAIL_SKIP reason=no_email userId={}", user.getId());
                return false;
            }
            return emailService.sendNotificationEmail(email, message);
        } catch (Exception e) {
            log.error("EMAIL_NOTIFICATION_FAIL userId={} error={}", user.getId(), e.getMessage());
            return false;
        }
    }

    private void saveInApp(User user, String message, String type, NotificationChannel channel) {
        try {
            Notification notification = Notification.builder()
                    .userId(user.getId())
                    .message(message)
                    .type(type)
                    .channelUsed(channel)
                    .read(false)
                    .build();
            notificationRepository.save(notification);
            log.info("NOTIFICATION_SAVED userId={} type={} channel={}", user.getId(), type, channel);
            // Also fire a Web Push so the alert appears even when the tab is closed
            webPushService.sendPush(user.getId(), type, message);
        } catch (Exception e) {
            log.error("NOTIFICATION_SAVE_FAIL userId={} error={}", user.getId(), e.getMessage());
        }
    }

    /**
     * Sends a goal-milestone notification when goal reaches 50%, 75%, or 100%.
     */
    public void sendGoalMilestoneNotification(User user, String goalTitle, int percent) {
        String emoji = percent >= 100 ? "🏆" : percent >= 75 ? "🔥" : "📈";
        String msg = String.format(
                "%s Goal milestone: \"%s\" is now %d%% complete! Keep going! 💪",
                emoji, goalTitle, percent);
        sendNotification(user, msg, "GOAL_MILESTONE");
    }
}
