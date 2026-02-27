package com.yourapp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Stores a browser Web Push subscription for one user+device.
 * A user can have multiple subscriptions (one per browser/device).
 */
@Document(collection = "push_subscriptions")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PushSubscription {

    @Id
    private String id;

    @Indexed
    private String userId;

    /** Full push service endpoint URL (FCM, APNS, etc.) */
    private String endpoint;

    /** Base64url-encoded client public key (p256dh) */
    private String p256dh;

    /** Base64url-encoded auth secret */
    private String auth;

    private LocalDateTime createdAt;
}
