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
