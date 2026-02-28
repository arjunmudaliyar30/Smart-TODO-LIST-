package com.yourapp.controller;

import com.yourapp.dto.ApiResponse;
import com.yourapp.model.Message;
import com.yourapp.model.User;
import com.yourapp.repository.MessageRepository;
import com.yourapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class MessageController {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    // -----------------------------------------------------------------------
    // Search users to start a conversation
    // -----------------------------------------------------------------------
    @GetMapping("/users/search")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> searchUsers(
            @AuthenticationPrincipal User me,
            @RequestParam String q) {

        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Not authenticated"));
        if (q == null || q.isBlank()) return ResponseEntity.ok(ApiResponse.success(List.of()));

        try {
            List<User> found = userRepository
                    .findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(q.trim(), q.trim());

            List<Map<String, String>> result = (found == null ? List.<User>of() : found).stream()
                    .filter(u -> u.getId() != null && !u.getId().equals(me.getId()))
                    .map(u -> {
                        Map<String, String> m = new LinkedHashMap<>();
                        m.put("id", u.getId());
                        m.put("name", u.getFullName() != null ? u.getFullName() : u.getEmail());
                        m.put("email", u.getEmail() != null ? u.getEmail() : "");
                        return m;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception ex) {
            log.error("Error searching users q={}: {}", q, ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Search failed"));
        }
    }

    // -----------------------------------------------------------------------
    // List conversations (unique peers the current user has chatted with)
    // -----------------------------------------------------------------------
    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getConversations(
            @AuthenticationPrincipal User me) {

        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Not authenticated"));
        try {
            List<Message> all = messageRepository
                    .findBySenderIdOrRecipientIdOrderByCreatedAtDesc(me.getId(), me.getId());
            if (all == null) all = Collections.emptyList();

            // Collect unique peer IDs (most recent first)
            LinkedHashMap<String, Message> peers = new LinkedHashMap<>();
            for (Message msg : all) {
                if (msg.getSenderId() == null || msg.getRecipientId() == null) continue;
                String peerId = msg.getSenderId().equals(me.getId())
                        ? msg.getRecipientId() : msg.getSenderId();
                if (peerId != null) peers.putIfAbsent(peerId, msg);
            }

            // Count total unread for this user once (not per-conversation)
            long totalUnread = messageRepository.countByRecipientIdAndReadFalse(me.getId());

            List<Map<String, Object>> result = new ArrayList<>();
            for (Map.Entry<String, Message> entry : peers.entrySet()) {
                userRepository.findById(entry.getKey()).ifPresent(peer -> {
                    Map<String, Object> conv = new LinkedHashMap<>();
                    conv.put("peerId",      peer.getId());
                    conv.put("peerName",    peer.getFullName() != null ? peer.getFullName() : peer.getEmail());
                    conv.put("peerEmail",   peer.getEmail() != null ? peer.getEmail() : "");
                    conv.put("lastMessage", entry.getValue().getContent() != null ? entry.getValue().getContent() : "");
                    conv.put("lastAt",      entry.getValue().getCreatedAt());
                    conv.put("unread",      totalUnread);
                    result.add(conv);
                });
            }

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception ex) {
            log.error("Error loading conversations for user {}: {}", me.getId(), ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Could not load conversations"));
        }
    }

    // -----------------------------------------------------------------------
    // Get full message thread between me and another user
    // -----------------------------------------------------------------------
    @GetMapping("/conversation/{peerId}")
    public ResponseEntity<ApiResponse<List<Message>>> getThread(
            @AuthenticationPrincipal User me,
            @PathVariable String peerId) {

        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Not authenticated"));
        if (peerId == null || peerId.isBlank()) return ResponseEntity.badRequest()
                .body(ApiResponse.error("peerId is required"));
        try {
            List<Message> msgs = messageRepository
                    .findBySenderIdAndRecipientIdOrSenderIdAndRecipientIdOrderByCreatedAtAsc(
                            me.getId(), peerId, peerId, me.getId());
            if (msgs == null) msgs = Collections.emptyList();

            // Mark incoming messages as read
            msgs.stream()
                    .filter(m -> me.getId().equals(m.getRecipientId()) && !m.isRead())
                    .forEach(m -> { m.setRead(true); messageRepository.save(m); });

            return ResponseEntity.ok(ApiResponse.success(msgs));
        } catch (Exception ex) {
            log.error("Error loading thread me={} peer={}: {}", me.getId(), peerId, ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Could not load thread"));
        }
    }

    // -----------------------------------------------------------------------
    // Send a message
    // -----------------------------------------------------------------------
    @PostMapping
    public ResponseEntity<ApiResponse<Message>> sendMessage(
            @AuthenticationPrincipal User me,
            @RequestBody Map<String, String> body) {

        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Not authenticated"));

        String recipientId = body != null ? body.get("recipientId") : null;
        String content     = body != null ? body.get("content")     : null;

        if (recipientId == null || recipientId.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("recipientId is required"));
        }
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("content is required"));
        }
        if (me.getId().equals(recipientId)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot send message to yourself"));
        }
        if (!userRepository.existsById(recipientId)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Recipient not found"));
        }
        try {
            Message msg = Message.builder()
                    .senderId(me.getId())
                    .recipientId(recipientId)
                    .content(content.trim())
                    .build();
            return ResponseEntity.ok(ApiResponse.success("Message sent", messageRepository.save(msg)));
        } catch (Exception ex) {
            log.error("Error sending message from {} to {}: {}", me.getId(), recipientId, ex.getMessage(), ex);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to send message"));
        }
    }

    // -----------------------------------------------------------------------
    // Unread count badge
    // -----------------------------------------------------------------------
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> unreadCount(@AuthenticationPrincipal User me) {
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Not authenticated"));
        try {
            long count = messageRepository.countByRecipientIdAndReadFalse(me.getId());
            return ResponseEntity.ok(ApiResponse.success(count));
        } catch (Exception ex) {
            log.error("Error counting unread for user {}: {}", me.getId(), ex.getMessage(), ex);
            return ResponseEntity.ok(ApiResponse.success(0L)); // degrade gracefully
        }
    }
}
