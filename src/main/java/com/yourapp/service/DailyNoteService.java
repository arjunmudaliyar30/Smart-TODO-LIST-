package com.yourapp.service;

import com.yourapp.dto.AutoSaveRequest;
import com.yourapp.dto.DailyNoteRequest;
import com.yourapp.dto.DailyNoteResponseDTO;
import com.yourapp.dto.ShareNoteRequest;
import com.yourapp.event.NoteWrittenEvent;
import com.yourapp.model.DailyNote;
import com.yourapp.model.DailyNoteShare;
import com.yourapp.model.PermissionType;
import com.yourapp.model.Task;
import com.yourapp.model.Workout;
import com.yourapp.repository.CaloriesLogRepository;
import com.yourapp.repository.DailyNoteRepository;
import com.yourapp.repository.DailyNoteShareRepository;
import com.yourapp.repository.TaskRepository;
import com.yourapp.repository.UserRepository;
import com.yourapp.repository.WorkoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class DailyNoteService {

    private final DailyNoteRepository      noteRepo;
    private final DailyNoteShareRepository shareRepo;
    private final TaskRepository           taskRepo;
    private final WorkoutRepository        workoutRepo;
    private final CaloriesLogRepository    caloriesLogRepo;
    private final UserRepository           userRepo;
    private final ApplicationEventPublisher eventPublisher;

    // -----------------------------------------------------------------------
    // CREATE / UPDATE
    // -----------------------------------------------------------------------

    @Transactional
    public DailyNote createOrUpdateNote(String userId, DailyNoteRequest req) {
        LocalDate date = req.getDate() != null ? req.getDate() : LocalDate.now();
        Optional<DailyNote> existing = noteRepo.findByUserIdAndDateAndDeletedFalse(userId, date);

        if (existing.isPresent()) {
            DailyNote note = existing.get();
            if (req.getContent()     != null) note.setContent(req.getContent());
            if (req.getMood()        != null) note.setMood(req.getMood());
            if (req.getEnergyLevel() != null) note.setEnergyLevel(req.getEnergyLevel());
            if (req.getSleepHours()  != null) note.setSleepHours(req.getSleepHours());
            note.setUpdatedAt(LocalDateTime.now());
            DailyNote saved = noteRepo.save(note);
            eventPublisher.publishEvent(new NoteWrittenEvent(this, userId, saved.getId(), date));
            return saved;
        } else {
            DailyNote note = DailyNote.builder()
                    .userId(userId)
                    .date(date)
                    .content(req.getContent())
                    .mood(req.getMood())
                    .energyLevel(req.getEnergyLevel())
                    .sleepHours(req.getSleepHours())
                    .createdAt(LocalDateTime.now())
                    .build();
            DailyNote saved = noteRepo.save(note);
            eventPublisher.publishEvent(new NoteWrittenEvent(this, userId, saved.getId(), date));
            return saved;
        }
    }

    // -----------------------------------------------------------------------
    // AUTO-SAVE (minimal, called every ~10 s from frontend)
    // -----------------------------------------------------------------------

    @Transactional
    @SuppressWarnings("null")
    public DailyNote autoSave(String userId, AutoSaveRequest req) {
        DailyNote note;

        if (req.getNoteId() != null && !req.getNoteId().isBlank()) {
            note = noteRepo.findById(req.getNoteId())
                    .filter(n -> n.getUserId().equals(userId) && !n.isDeleted())
                    .orElseThrow(() -> new RuntimeException("Note not found"));
        } else if (req.getDate() != null) {
            note = noteRepo.findByUserIdAndDateAndDeletedFalse(userId, req.getDate())
                    .orElseGet(() -> DailyNote.builder()
                            .userId(userId)
                            .date(req.getDate())
                            .createdAt(LocalDateTime.now())
                            .build());
        } else {
            throw new RuntimeException("Either noteId or date is required");
        }

        if (req.getContent() != null) note.setContent(req.getContent());
        note.setUpdatedAt(LocalDateTime.now());
        return noteRepo.save(note);
    }

    // -----------------------------------------------------------------------
    // GET SINGLE NOTE
    // -----------------------------------------------------------------------

    @SuppressWarnings("null")
    public DailyNoteResponseDTO getNote(String noteId, String currentUserId) {
        DailyNote note = noteRepo.findById(noteId)
                .filter(n -> !n.isDeleted())
                .orElseThrow(() -> new RuntimeException("Note not found or deleted"));

        boolean isOwner = note.getUserId().equals(currentUserId);
        PermissionType perm = null;

        if (!isOwner) {
            DailyNoteShare share = shareRepo
                    .findByNoteIdAndSharedWithUserId(noteId, currentUserId)
                    .orElseThrow(() -> new RuntimeException("Access denied"));
            perm = share.getPermissionType();
        }

        return buildResponse(note, isOwner, perm);
    }

    public DailyNoteResponseDTO getNoteByDate(String userId, LocalDate date) {
        DailyNote note = noteRepo.findByUserIdAndDateAndDeletedFalse(userId, date)
                .orElseGet(() -> DailyNote.builder()
                        .userId(userId)
                        .date(date)
                        .build());
        return buildResponse(note, true, null);
    }

    // -----------------------------------------------------------------------
    // SEARCH
    // -----------------------------------------------------------------------

    public List<DailyNoteResponseDTO> search(String userId, String keyword,
                                             LocalDate startDate, LocalDate endDate) {
        List<DailyNote> owned;
        if (keyword != null && !keyword.isBlank()) {
            owned = noteRepo.searchByContent(userId, keyword);
        } else {
            LocalDate s = startDate != null ? startDate : LocalDate.now().minusMonths(3);
            LocalDate e = endDate   != null ? endDate   : LocalDate.now();
            owned = noteRepo.findByUserIdAndDeletedFalseAndDateBetweenOrderByDateDesc(userId, s, e);
        }

        // Also include notes shared with the user
        List<DailyNoteShare> shares = shareRepo.findBySharedWithUserId(userId);
        List<String> sharedIds = shares.stream()
                .map(DailyNoteShare::getNoteId).collect(Collectors.toList());
        List<DailyNote> sharedNotes = sharedIds.isEmpty()
                ? List.of() : noteRepo.findByIdInAndDeletedFalse(sharedIds);

        List<DailyNoteResponseDTO> result = owned.stream()
                .map(n -> buildResponse(n, true, null))
                .collect(Collectors.toList());

        for (DailyNote sn : sharedNotes) {
            PermissionType perm = shares.stream()
                    .filter(s -> s.getNoteId().equals(sn.getId()))
                    .findFirst()
                    .map(DailyNoteShare::getPermissionType)
                    .orElse(PermissionType.VIEW);
            result.add(buildResponse(sn, false, perm));
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // SHARE / REVOKE
    // -----------------------------------------------------------------------

    @SuppressWarnings("null")
    public DailyNoteShare shareNote(String noteId, String ownerId, ShareNoteRequest req) {
        // Verify note exists and owner has access
        noteRepo.findById(noteId)
                .filter(n -> n.getUserId().equals(ownerId) && !n.isDeleted())
                .orElseThrow(() -> new RuntimeException("Note not found or access denied"));

        if (req.getTargetUserId().equals(ownerId)) {
            throw new RuntimeException("Cannot share a note with yourself");
        }

        if (!userRepo.existsById(req.getTargetUserId())) {
            throw new RuntimeException("Target user not found");
        }

        // Upsert — if share already exists, update permission
        DailyNoteShare share = shareRepo
                .findByNoteIdAndSharedWithUserId(noteId, req.getTargetUserId())
                .orElse(DailyNoteShare.builder()
                        .noteId(noteId)
                        .ownerId(ownerId)
                        .sharedWithUserId(req.getTargetUserId())
                        .build());

        share.setPermissionType(req.getPermissionType());
        return shareRepo.save(share);
    }

    @SuppressWarnings("null")
    public void revokeShare(String noteId, String ownerId, String targetUserId) {
        noteRepo.findById(noteId)
                .filter(n -> n.getUserId().equals(ownerId))
                .orElseThrow(() -> new RuntimeException("Note not found or access denied"));
        shareRepo.deleteByNoteIdAndSharedWithUserId(noteId, targetUserId);
    }

    // -----------------------------------------------------------------------
    // SOFT DELETE
    // -----------------------------------------------------------------------

    @Transactional
    @SuppressWarnings("null")
    public void softDelete(String noteId, String ownerId) {
        DailyNote note = noteRepo.findById(noteId)
                .filter(n -> n.getUserId().equals(ownerId) && !n.isDeleted())
                .orElseThrow(() -> new RuntimeException("Note not found or access denied"));
        note.setDeleted(true);
        note.setUpdatedAt(LocalDateTime.now());
        noteRepo.save(note);
        shareRepo.deleteByNoteId(noteId); // also clean up shares
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private DailyNoteResponseDTO buildResponse(DailyNote note, boolean isOwner, PermissionType perm) {
        int tasksDone = 0, workoutsDone = 0, caloriesNet = 0;

        if (note.getId() != null && note.getDate() != null) {
            String uid = note.getUserId();
            LocalDate d = note.getDate();

            // Tasks completed on this date
            try {
                tasksDone = (int) taskRepo.findByUserIdOrderByCreatedAtDesc(uid).stream()
                        .filter(t -> (Task.TaskStatus.DONE.equals(t.getStatus())
                                || Task.TaskStatus.COMPLETED.equals(t.getStatus()))
                                && d.equals(t.getScheduledDate()))
                        .count();
            } catch (Exception e) {
                log.warn("Could not compute tasksDone for note {}: {}", note.getId(), e.getMessage());
            }

            // Workouts completed on this date
            try {
                workoutsDone = (int) workoutRepo.findByUserIdOrderByCreatedAtDesc(uid).stream()
                        .filter(w -> Workout.WorkoutStatus.COMPLETED.equals(w.getStatus())
                                && d.equals(w.getWorkoutDate()))
                        .count();
            } catch (Exception e) {
                log.warn("Could not compute workoutsDone for note {}: {}", note.getId(), e.getMessage());
            }

            // Calories net (consumed - burned) on this date
            try {
                var logs = caloriesLogRepo.findByUserIdAndDateOrderByCreatedAtAsc(uid, d);
                int consumed = logs.stream().mapToInt(cl -> cl.getConsumed()).sum();
                int burned   = logs.stream().mapToInt(cl -> cl.getBurned()).sum();
                caloriesNet  = consumed - burned;
            } catch (Exception e) {
                log.warn("Could not compute caloriesNet for note {}: {}", note.getId(), e.getMessage());
            }
        }

        return DailyNoteResponseDTO.builder()
                .id(note.getId())
                .date(note.getDate())
                .content(note.getContent())
                .mood(note.getMood())
                .isOwner(isOwner)
                .permissionType(perm)
                .tasksCompletedToday(tasksDone)
                .workoutsCompletedToday(workoutsDone)
                .caloriesNetToday(caloriesNet)
                .build();
    }
}
