package com.yourapp.controller;

import com.yourapp.dto.AiChatRequest;
import com.yourapp.dto.AiChatResponse;
import com.yourapp.dto.ApiResponse;
import com.yourapp.model.ChatMessage;
import com.yourapp.model.User;
import com.yourapp.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /**
     * Send a message to the AI assistant.
     * If the AI detects a scheduling intent, it creates the task and returns it
     * in {@code data.taskCreated} so the frontend can refresh the task list.
     */
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AiChatRequest request) throws IOException {

        AiChatResponse response = aiService.chat(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/chat/history")
    public ResponseEntity<ApiResponse<List<ChatMessage>>> getChatHistory(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String sessionId) {

        List<ChatMessage> history = aiService.getChatHistory(user.getId(), sessionId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @DeleteMapping("/chat/session/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> clearSession(
            @AuthenticationPrincipal User user,
            @PathVariable String sessionId) {

        aiService.clearSession(user.getId(), sessionId);
        return ResponseEntity.ok(ApiResponse.success("Session cleared", null));
    }

    /**
     * Generate an end-of-day summary based on the user's actual tasks and goals.
     * GET /api/ai/daily-summary
     */
    @GetMapping("/daily-summary")
    public ResponseEntity<ApiResponse<String>> dailySummary(
            @AuthenticationPrincipal User user) throws IOException {

        String summary = aiService.generateDailySummary(user.getId(), user.getFullName());
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * Quick 1-2 sentence AI tip for a given context.
     * POST /api/ai/suggest  { "context": "..." }
     */
    @PostMapping("/suggest")
    public ResponseEntity<ApiResponse<String>> quickSuggest(
            @AuthenticationPrincipal User user,
            @RequestBody java.util.Map<String, String> body) throws IOException {

        String context = body.getOrDefault("context", "");
        String tip = aiService.quickSuggest(context);
        return ResponseEntity.ok(ApiResponse.success(tip));
    }
}
