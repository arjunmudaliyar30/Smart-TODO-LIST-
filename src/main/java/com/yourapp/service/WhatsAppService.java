package com.yourapp.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WhatsAppService {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.whatsapp.from:whatsapp:+14155238886}")
    private String fromNumber;

    @Value("${twilio.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
    }

    /**
     * Send a WhatsApp message with automatic retry on transient failures.
     * Retries up to {@code twilio.retry.max-attempts} times (default 3)
     * with exponential backoff (1s, 2s, 4s…).
     *
     * @param toPhoneNumber recipient in E.164 format e.g. +1234567890
     * @param messageBody   text content
     */
    public void sendWhatsAppMessage(String toPhoneNumber, String messageBody) {
        if (toPhoneNumber == null || toPhoneNumber.isBlank()) {
            log.warn("WHATSAPP_SKIP reason=blank_phone");
            return;
        }

        int attempt = 0;
        while (attempt < maxRetryAttempts) {
            attempt++;
            try {
                Message message = Message.creator(
                        new PhoneNumber("whatsapp:" + toPhoneNumber),
                        new PhoneNumber(fromNumber),
                        messageBody
                ).create();

                log.info("WHATSAPP_SENT to={} sid={} status={} attempt={}",
                        toPhoneNumber, message.getSid(), message.getStatus(), attempt);
                return; // success — stop retrying

            } catch (Exception e) {
                log.warn("WHATSAPP_FAIL to={} attempt={}/{} error={}",
                        toPhoneNumber, attempt, maxRetryAttempts, e.getMessage());

                if (attempt < maxRetryAttempts) {
                    long backoffMs = (long) Math.pow(2, attempt - 1) * 1000L;
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    log.error("WHATSAPP_EXHAUSTED to={} after {} attempts", toPhoneNumber, maxRetryAttempts);
                }
            }
        }
    }

    public void sendTaskReminder(String toPhoneNumber, String taskTitle, String dueDate) {
        String body = String.format(
                "⏰ *Task Reminder* — AI Execution System\n\n" +
                "Your task *\"%s\"* is due on *%s*.\n\n" +
                "Stay focused and get it done! 💪",
                taskTitle, dueDate);
        sendWhatsAppMessage(toPhoneNumber, body);
    }
}
