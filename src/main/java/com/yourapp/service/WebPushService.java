package com.yourapp.service;

import com.yourapp.model.PushSubscription;
import com.yourapp.repository.PushSubscriptionRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Sends Web Push notifications via the VAPID protocol.
 * Notifications pop up as OS-level alerts even when the browser tab is closed.
 */
@Service
@Slf4j
public class WebPushService {

    private final PushSubscriptionRepository subscriptionRepository;

    @Value("${vapid.public.key:}")
    private String configuredPublicKey;

    @Value("${vapid.private.key:}")
    private String configuredPrivateKey;

    @Value("${vapid.subject:mailto:arjunmudaliyar99@gmail.com}")
    private String subject;

    private PushService pushService;
    private String effectivePublicKey;

    public WebPushService(PushSubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @PostConstruct
    public void init() {
        try {
            if (Security.getProvider("BC") == null) {
                Security.addProvider(new BouncyCastleProvider());
            }

            if (configuredPublicKey == null || configuredPublicKey.isBlank()
                    || configuredPrivateKey == null || configuredPrivateKey.isBlank()) {
                // Auto-generate VAPID keys using BouncyCastle EC P-256
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", "BC");
                keyGen.initialize(new ECGenParameterSpec("prime256v1"));
                KeyPair keyPair = keyGen.generateKeyPair();

                BCECPublicKey  pubKey  = (BCECPublicKey)  keyPair.getPublic();
                BCECPrivateKey privKey = (BCECPrivateKey) keyPair.getPrivate();

                // Uncompressed EC point: 0x04 || X || Y (65 bytes)
                byte[] pubBytes = pubKey.getQ().getEncoded(false);

                // Private key D value — ensure exactly 32 bytes (strip BigInteger sign byte)
                byte[] privRaw = privKey.getD().toByteArray();
                if (privRaw.length > 32) {
                    privRaw = Arrays.copyOfRange(privRaw, privRaw.length - 32, privRaw.length);
                } else if (privRaw.length < 32) {
                    byte[] padded = new byte[32];
                    System.arraycopy(privRaw, 0, padded, 32 - privRaw.length, privRaw.length);
                    privRaw = padded;
                }

                effectivePublicKey = Base64.getUrlEncoder().withoutPadding().encodeToString(pubBytes);
                String effectivePrivateKey = Base64.getUrlEncoder().withoutPadding().encodeToString(privRaw);

                log.warn("=== VAPID keys auto-generated. Paste into application.properties to persist: ===");
                log.warn("vapid.public.key={}", effectivePublicKey);
                log.warn("vapid.private.key={}", effectivePrivateKey);

                pushService = new PushService(effectivePublicKey, effectivePrivateKey, subject);
            } else {
                effectivePublicKey = configuredPublicKey;
                pushService = new PushService(configuredPublicKey, configuredPrivateKey, subject);
            }

            log.info("WebPushService initialized. VAPID public key: {}", effectivePublicKey);
        } catch (Exception e) {
            log.error("WebPushService init failed: {}", e.getMessage(), e);
        }
    }

    /** Returns the VAPID public key to send to the browser for subscription. */
    public String getPublicKey() {
        return effectivePublicKey;
    }

    /** Save or update a browser push subscription for a user. */
    public void subscribe(String userId, String endpoint, String p256dh, String auth) {
        subscriptionRepository.findByUserIdAndEndpoint(userId, endpoint).ifPresentOrElse(
                existing -> log.debug("PUSH_SUB_EXISTS userId={}", userId),
                () -> {
                    subscriptionRepository.save(PushSubscription.builder()
                            .userId(userId)
                            .endpoint(endpoint)
                            .p256dh(p256dh)
                            .auth(auth)
                            .createdAt(LocalDateTime.now())
                            .build());
                    log.info("PUSH_SUB_SAVED userId={}", userId);
                }
        );
    }

    /** Remove a push subscription (called when user unsubscribes or the endpoint 410s). */
    public void unsubscribe(String userId, String endpoint) {
        subscriptionRepository.deleteByUserIdAndEndpoint(userId, endpoint);
        log.info("PUSH_SUB_REMOVED userId={}", userId);
    }

    /**
     * Send a Web Push notification to all registered devices for a user.
     * Payload is a JSON string that the service worker uses to display the alert.
     */
    public void sendPush(String userId, String title, String body) {
        if (pushService == null) {
            log.warn("PUSH_SKIP reason=pushService_not_initialized userId={}", userId);
            return;
        }

        List<PushSubscription> subs = subscriptionRepository.findByUserId(userId);
        if (subs.isEmpty()) {
            log.debug("PUSH_SKIP reason=no_subscriptions userId={}", userId);
            return;
        }

        String payload = "{\"title\":\"" + escapeJson(title) + "\",\"body\":\"" + escapeJson(body) + "\"}";

        for (PushSubscription sub : subs) {
            try {
                Notification notification = new Notification(
                        sub.getEndpoint(),
                        sub.getP256dh(),
                        sub.getAuth(),
                        payload.getBytes(StandardCharsets.UTF_8)
                );
                pushService.send(notification);
                log.info("PUSH_SENT userId={} title={}", userId, title);
            } catch (Exception e) {
                log.error("PUSH_FAIL userId={} error={}", userId, e.getMessage());
                // Remove stale/expired subscription
                subscriptionRepository.delete(sub);
                log.info("PUSH_SUB_DELETED_STALE userId={}", userId);
            }
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
