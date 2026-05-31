package com.yourapp.controller;

import com.yourapp.dto.ApiResponse;
import com.yourapp.model.User;
import com.yourapp.service.PartnershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/partnership")
@RequiredArgsConstructor
public class PartnershipController {

    private final PartnershipService partnershipService;

    /** POST /api/partnership/invite — body: { "email": "partner@email.com" } */
    @PostMapping("/invite")
    public ResponseEntity<ApiResponse<Object>> invite(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "").trim();
        if (email.isEmpty()) return ResponseEntity.badRequest().body(ApiResponse.error("email is required"));
        return ResponseEntity.ok(ApiResponse.success("Invite sent",
                partnershipService.invite(user.getId(), email)));
    }

    /** GET /api/partnership/mine */
    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<Object>> getMine(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(partnershipService.getMine(user.getId())));
    }

    /** PATCH /api/partnership/{id}/accept */
    @PatchMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<Object>> accept(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Partnership accepted",
                partnershipService.accept(user.getId(), id)));
    }

    /** PATCH /api/partnership/{id}/decline */
    @PatchMapping("/{id}/decline")
    public ResponseEntity<ApiResponse<Object>> decline(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Partnership declined",
                partnershipService.decline(user.getId(), id)));
    }

    /** GET /api/partnership/leaderboard */
    @GetMapping("/leaderboard")
    public ResponseEntity<ApiResponse<Object>> getLeaderboard(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(partnershipService.getLeaderboard(user.getId())));
    }

    /** POST /api/partnership/encourage — body: { "partnershipId": "..." } */
    @PostMapping("/encourage")
    public ResponseEntity<ApiResponse<Object>> encourage(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        String pId = body.getOrDefault("partnershipId", "");
        if (pId.isEmpty()) return ResponseEntity.badRequest().body(ApiResponse.error("partnershipId is required"));
        partnershipService.encourage(user.getId(), pId);
        return ResponseEntity.ok(ApiResponse.success("Encouragement sent!", null));
    }

    /** GET /api/partnership/challenge */
    @GetMapping("/challenge")
    public ResponseEntity<ApiResponse<Object>> getChallenge(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(partnershipService.getChallenge(user.getId())));
    }
}
