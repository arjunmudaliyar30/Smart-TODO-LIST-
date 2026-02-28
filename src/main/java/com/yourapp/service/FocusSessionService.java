package com.yourapp.service;

import com.yourapp.dto.FocusSessionDTO;
import com.yourapp.event.FocusSessionCompletedEvent;
import com.yourapp.model.FocusSession;
import com.yourapp.model.Task.TaskStatus;
import com.yourapp.repository.FocusSessionRepository;
import com.yourapp.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Phase 8: Focus Mode Service
 * Manages focus sessions — start, stop, and scheduled expiry.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class FocusSessionService {

    private final FocusSessionRepository   focusSessionRepository;
    private final TaskRepository           taskRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** Start a new focus session for the user. */
    public FocusSession startSession(String userId, int durationMinutes, String linkedTaskId) {
        if (durationMinutes < 1 || durationMinutes > 480)
            throw new IllegalArgumentException("Duration must be between 1 and 480 minutes");

        LocalDateTime now = LocalDateTime.now();
        FocusSession session = FocusSession.builder()
                .userId(userId)
                .startTime(now)
                .endTime(now.plusMinutes(durationMinutes))
                .durationMinutes(durationMinutes)
                .linkedTaskId(linkedTaskId)
                .build();
        return focusSessionRepository.save(session);
    }

    /** Manually complete a session before it expires. */
    @SuppressWarnings("null")
    public FocusSession completeSession(String userId, String sessionId) {
        FocusSession session = focusSessionRepository.findById(sessionId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Focus session not found"));

        if (!session.isCompleted() && !session.isExpired()) {
            session.setCompleted(true);
            session = focusSessionRepository.save(session);
            onSessionCompleted(session);
        }
        return session;
    }

    public List<FocusSessionDTO> getUserSessions(String userId) {
        return focusSessionRepository.findByUserId(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /** Scheduled: complete any sessions whose endTime has passed. */
    public void processExpiredSessions() {
        List<FocusSession> expired = focusSessionRepository
                .findByCompletedFalseAndExpiredFalseAndEndTimeBefore(LocalDateTime.now());

        for (FocusSession session : expired) {
            session.setCompleted(true);
            focusSessionRepository.save(session);
            onSessionCompleted(session);
            log.info("Auto-completed focus session {} for user {}", session.getId(), session.getUserId());
        }
    }

    @SuppressWarnings("null")
    private void onSessionCompleted(FocusSession session) {
        // Mark linked task done if present
        if (session.getLinkedTaskId() != null) {
            taskRepository.findById(session.getLinkedTaskId()).ifPresent(task -> {
                if (task.getStatus() != TaskStatus.DONE && task.getStatus() != TaskStatus.COMPLETED) {
                    task.setStatus(TaskStatus.DONE);
                    task.setCompletedAt(LocalDateTime.now());
                    taskRepository.save(task);
                }
            });
        }

        // Fire event for streak/score update
        eventPublisher.publishEvent(new FocusSessionCompletedEvent(
                this,
                session.getUserId(),
                session.getId(),
                session.getLinkedTaskId(),
                session.getStartTime().toLocalDate()
        ));
    }

    private FocusSessionDTO toDTO(FocusSession s) {
        return FocusSessionDTO.builder()
                .id(s.getId())
                .userId(s.getUserId())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .durationMinutes(s.getDurationMinutes())
                .linkedTaskId(s.getLinkedTaskId())
                .completed(s.isCompleted())
                .expired(s.isExpired())
                .build();
    }
}
