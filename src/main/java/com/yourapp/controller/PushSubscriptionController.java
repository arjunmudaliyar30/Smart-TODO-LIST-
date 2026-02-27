package com.yourapp.controller;

import com.yourapp.repository.UserRepository;
import com.yourapp.service.WebPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushSubscriptionController {

    private final WebPushService webPushService;
    private final UserRepository userRepository;

    /** Frontend fetches this to call PushManager.subscribe({applicationServerKey: ...}) */
    @GetMapping("/public-key")
    public ResponseEntity<Map<String, String>> getPublicKey() {
        return ResponseEntity.ok(Map.of("publicKey", webPushService.getPublicKey()));
    }

    /**
     * Browser sends this after subscribing via PushManager.subscribe().
     * Body: { "endpoint": "...", "p256dh": "...", "auth": "..." }
     */
    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, String>> subscribe(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody Map<String, String> body) {

        String userId = userRepository.findByEmail(principal.getUsername())
                .map(u -> u.getId()).orElse(principal.getUsername());
        String endpoint = body.get("endpoint");
        String p256dh   = body.get("p256dh");
        String auth     = body.get("auth");

        if (endpoint == null || p256dh == null || auth == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Missing subscription fields"));
        }

        webPushService.subscribe(userId, endpoint, p256dh, auth);
        return ResponseEntity.ok(Map.of("message", "Subscribed"));
    }

    /** Called when the user explicitly revokes push permission, or on unsubscribe. */
    @PostMapping("/unsubscribe")
    public ResponseEntity<Map<String, String>> unsubscribe(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody Map<String, String> body) {

        String userId = userRepository.findByEmail(principal.getUsername())
                .map(u -> u.getId()).orElse(principal.getUsername());
        webPushService.unsubscribe(userId, body.get("endpoint"));
        return ResponseEntity.ok(Map.of("message", "Unsubscribed"));
    }
}
