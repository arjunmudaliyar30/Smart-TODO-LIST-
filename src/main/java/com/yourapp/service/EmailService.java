package com.yourapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@aiexecution.app}")
    private String fromAddress;

    /**
     * Send a plain-text email notification.
     *
     * @param toEmail   recipient email address
     * @param subject   email subject
     * @param body      plain-text body
     * @return true if sent successfully, false on any failure
     */
    public boolean sendEmail(String toEmail, String subject, String body) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("EMAIL_SKIP reason=blank_recipient");
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("EMAIL_SENT to={} subject={}", toEmail, subject);
            return true;
        } catch (MailException e) {
            log.error("EMAIL_FAIL to={} error={}", toEmail, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("EMAIL_ERROR to={} error={}", toEmail, e.getMessage());
            return false;
        }
    }

    /**
     * Convenience method for notification emails with a default subject.
     */
    public boolean sendNotificationEmail(String toEmail, String message) {
        return sendEmail(toEmail, "AI Execution System — Notification", message);
    }
}
