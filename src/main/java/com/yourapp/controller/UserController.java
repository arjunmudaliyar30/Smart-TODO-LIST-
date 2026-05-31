package com.yourapp.controller;

import com.yourapp.dto.ApiResponse;
import com.yourapp.model.User;
import com.yourapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * User profile operations.
 *
 * GET   /api/users/me            — get current user profile
 * PATCH /api/users/me/accountability — toggle accountability mode
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class UserController {

    private final UserRepository userRepository;

    /** Returns the authenticated user's public profile. */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> getMe(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved", user));
    }

    /**
     * Returns minimal public info (id, name, email) for any user by ID.
     * Used by the UI to resolve collaborator IDs to display names.
     */
    @GetMapping("/{id}/public")
    public ResponseEntity<ApiResponse<Map<String, String>>> getPublicProfile(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("Not found",
                    Map.of("id", id, "name", "Unknown", "email", "")));
        }
        User u = opt.get();
        Map<String, String> info = new LinkedHashMap<>();
        info.put("id",    u.getId());
        info.put("name",  u.getFullName() != null ? u.getFullName() : u.getEmail());
        info.put("email", u.getEmail());
        return ResponseEntity.ok(ApiResponse.success("OK", info));
    }

    /**
     * PATCH /api/users/me/preferences
     * Updates user notification & UI preferences.
     * Body: { "pushNotificationsEnabled": true, "dailySummaryEnabled": true,
     *         "dailySummaryHour": 8, "emailNotificationsEnabled": true,
     *         "aiTone": "friendly" }
     */
    @PatchMapping("/me/preferences")
    public ResponseEntity<ApiResponse<User>> updatePreferences(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {

        if (body.containsKey("pushNotificationsEnabled")) {
            user.setPushNotificationsEnabled(
                    Boolean.parseBoolean(body.get("pushNotificationsEnabled").toString()));
        }
        if (body.containsKey("dailySummaryEnabled")) {
            user.setDailySummaryEnabled(
                    Boolean.parseBoolean(body.get("dailySummaryEnabled").toString()));
        }
        if (body.containsKey("dailySummaryHour")) {
            user.setDailySummaryHour(
                    Integer.parseInt(body.get("dailySummaryHour").toString()));
        }
        if (body.containsKey("emailNotificationsEnabled") || body.containsKey("aiTone")) {
            java.util.Map<String, String> prefs = user.getPreferences();
            if (prefs == null) prefs = new java.util.HashMap<>();
            if (body.containsKey("emailNotificationsEnabled")) {
                prefs.put("emailNotificationsEnabled", body.get("emailNotificationsEnabled").toString());
            }
            if (body.containsKey("aiTone")) {
                prefs.put("aiTone", body.get("aiTone").toString());
            }
            user.setPreferences(prefs);
        }

        User saved = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Preferences updated", saved));
    }

    /**
     * Enables or disables accountability mode and optionally sets a partner.
     *
     * Body: { "enabled": true/false, "partnerId": "user-id-or-null" }
     */
    @PatchMapping("/me/accountability")
    public ResponseEntity<ApiResponse<User>> updateAccountability(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {

        if (body.containsKey("enabled")) {
            user.setAccountabilityModeEnabled(
                    Boolean.parseBoolean(body.get("enabled").toString()));
        }
        if (body.containsKey("partnerId")) {
            Object partnerIdVal = body.get("partnerId");
            user.setAccountabilityPartnerId(
                    partnerIdVal == null ? null : partnerIdVal.toString());
        }

        User saved = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Accountability mode updated", saved));
    }
}
