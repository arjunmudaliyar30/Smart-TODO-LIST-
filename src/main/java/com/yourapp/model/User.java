package com.yourapp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * MongoDB document representing a user of the AI Execution System.
 *
 * Covers:
 *  - Core authentication (email, hashed password, role, account status)
 *  - Profile information (name, avatar, timezone, bio)
 *  - Long-term goal / vision tracking (longTermVision, currentFocus, lifeGoals)
 *  - Notification preferences (WhatsApp, daily summary toggle)
 *  - Audit fields (createdAt, lastLogin, lastActive)
 *  - Device / session tracking for "stay logged in" support
 */
@Document(collection = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    // ------------------------------------------------------------------
    // Identity
    // ------------------------------------------------------------------

    @Id
    private String id;

    private String fullName;

    @Indexed(unique = true)
    private String email;

    /** BCrypt-hashed password — never stored in plain text. */
    private String password;

    /** E.164 phone number for WhatsApp reminders, e.g. +1234567890 */
    private String phoneNumber;

    /** Optional URL or base64 data URI for profile picture. */
    private String avatarUrl;

    /** Short user bio / description of themselves. */
    private String bio;

    /** IANA timezone, e.g. "America/New_York". Used for reminders scheduling. */
    @Builder.Default
    private String timezone = "UTC";

    // ------------------------------------------------------------------
    // Authentication & Access Control
    // ------------------------------------------------------------------

    /** Spring Security role, e.g. ROLE_USER, ROLE_ADMIN. */
    @Builder.Default
    private String role = "ROLE_USER";

    /** Current account status. Only ACTIVE accounts can log in. */
    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    /**
     * Opaque refresh-token value stored here so the client can exchange it
     * for a new JWT without re-entering credentials.  Rotated on every use.
     */
    private String refreshToken;

    /** Expiry of the refresh token. */
    private LocalDateTime refreshTokenExpiry;

    // ------------------------------------------------------------------
    // Password Reset (OTP)
    // ------------------------------------------------------------------

    /** 6-digit OTP sent by email for password reset. */
    private String passwordResetOtp;

    /** Expiry timestamp for the password-reset OTP (valid 10 minutes). */
    private LocalDateTime otpExpiry;

    // ------------------------------------------------------------------
    // Long-term Goal & Vision Tracking
    // ------------------------------------------------------------------

    /**
     * The user's long-term vision statement — where they want to be in
     * 5-10 years. Stored as free text; can be analysed by the AI service.
     */
    private String longTermVision;

    /**
     * What the user is focused on this month / quarter.
     * Updated regularly; used to personalise AI chat responses.
     */
    private String currentFocus;

    /**
     * High-level life goal titles (e.g. "Start a SaaS", "Run a marathon").
     * More granular goals are stored in the separate goals collection.
     */
    @Builder.Default
    private List<String> lifeGoals = new ArrayList<>();

    /** Target date for the user's primary life goal. */
    private LocalDate primaryGoalTargetDate;

    /** AI-generated summary / analysis of the user's goals and vision. */
    private String aiVisionInsight;

    /** Date the AI vision insight was last refreshed. */
    private LocalDateTime aiVisionInsightUpdatedAt;

    // ------------------------------------------------------------------
    // Notification Preferences
    // ------------------------------------------------------------------

    /** Preferred delivery channel for all system notifications. */
    @Builder.Default
    private NotificationChannel preferredNotificationChannel = NotificationChannel.IN_APP;

    /** Master toggle for WhatsApp reminders. */
    @Builder.Default
    private boolean whatsappRemindersEnabled = false;

    /** Send a daily task summary via WhatsApp every morning. */
    @Builder.Default
    private boolean dailySummaryEnabled = true;

    /** Preferred hour (0-23, server's local time) for daily summary. */
    @Builder.Default
    private int dailySummaryHour = 8;

    /** Toggle for in-app (browser) push notifications. */
    @Builder.Default
    private boolean pushNotificationsEnabled = false;

    // ------------------------------------------------------------------
    // UI / App Preferences
    // ------------------------------------------------------------------

    /** UI theme preference: "dark" (default) or "light". */
    @Builder.Default
    private String theme = "dark";

    /** Arbitrary key-value map for future preference fields. */
    @Builder.Default
    private Map<String, String> preferences = new HashMap<>();

    // ------------------------------------------------------------------
    // Audit / Session
    // ------------------------------------------------------------------

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime lastLogin;

    /** Timestamp of the last API activity — used for session health checks. */
    private LocalDateTime lastActive;

    /** Total number of completed tasks — quick-access stat. */
    @Builder.Default
    private int totalTasksCompleted = 0;

    /** Total number of workouts logged. */
    @Builder.Default
    private int totalWorkoutsLogged = 0;

    // ------------------------------------------------------------------
    // Accountability Mode (Phase 5)
    // ------------------------------------------------------------------

    /**
     * When true: overdue tasks notify collaborators, broken streaks trigger
     * motivational alerts to the accountability partner.
     */
    @Builder.Default
    private boolean accountabilityModeEnabled = false;

    /** MongoDB ID of the user's designated accountability partner. Nullable. */
    private String accountabilityPartnerId;

    // ------------------------------------------------------------------
    // Account Status Enum
    // ------------------------------------------------------------------

    public enum AccountStatus {
        /** Normal, fully functional account. */
        ACTIVE,
        /** Temporarily suspended by admin. */
        SUSPENDED,
        /** User requested deletion — pending hard-delete job. */
        PENDING_DELETION
    }

    // ------------------------------------------------------------------
    // UserDetails contract
    // ------------------------------------------------------------------

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() {
        return accountStatus != AccountStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        return accountStatus == AccountStatus.ACTIVE;
    }
}
