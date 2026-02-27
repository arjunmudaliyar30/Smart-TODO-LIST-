package com.yourapp.controller;

import com.yourapp.dto.ApiResponse;
import com.yourapp.dto.AutoSaveRequest;
import com.yourapp.dto.DailyNoteRequest;
import com.yourapp.dto.DailyNoteResponseDTO;
import com.yourapp.dto.ShareNoteRequest;
import com.yourapp.model.DailyNote;
import com.yourapp.model.DailyNoteShare;
import com.yourapp.model.User;
import com.yourapp.service.DailyNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class DailyNoteController {

    private final DailyNoteService noteService;

    /** POST /api/notes — create or update note (upsert by userId+date). */
    @PostMapping
    public ResponseEntity<ApiResponse<DailyNote>> createOrUpdate(
            @AuthenticationPrincipal User user,
            @RequestBody DailyNoteRequest req) {

        DailyNote note = noteService.createOrUpdateNote(user.getId(), req);
        return ResponseEntity.ok(ApiResponse.success(note));
    }

    /** GET /api/notes/today — get (or create empty) note for today. */
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<DailyNoteResponseDTO>> getToday(
            @AuthenticationPrincipal User user) {

        DailyNoteResponseDTO dto = noteService.getNoteByDate(user.getId(), LocalDate.now());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /**
     * GET /api/notes?date=YYYY-MM-DD — get (or create empty) note for a given date.
     * If no date given, defaults to today.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<DailyNoteResponseDTO>> getByDate(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate target = date != null ? date : LocalDate.now();
        DailyNoteResponseDTO dto = noteService.getNoteByDate(user.getId(), target);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /** GET /api/notes/{id} — fetch a specific note by id (owner or shared viewer). */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DailyNoteResponseDTO>> getById(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {

        DailyNoteResponseDTO dto = noteService.getNote(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /**
     * PATCH /api/notes/autosave — lightweight auto-save (partial update).
     * Body: { noteId?, date?, content }
     */
    @PatchMapping("/autosave")
    public ResponseEntity<ApiResponse<DailyNote>> autoSave(
            @AuthenticationPrincipal User user,
            @RequestBody AutoSaveRequest req) {

        DailyNote note = noteService.autoSave(user.getId(), req);
        return ResponseEntity.ok(ApiResponse.success(note));
    }

    /**
     * GET /api/notes/search?keyword=&startDate=&endDate=
     * Search owned + shared notes by content, optionally filtered by date range.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<DailyNoteResponseDTO>>> search(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<DailyNoteResponseDTO> results = noteService.search(
                user.getId(), keyword, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /** POST /api/notes/{id}/share — share a note with another user. */
    @PostMapping("/{id}/share")
    public ResponseEntity<ApiResponse<DailyNoteShare>> share(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @RequestBody ShareNoteRequest req) {

        DailyNoteShare share = noteService.shareNote(id, user.getId(), req);
        return ResponseEntity.ok(ApiResponse.success(share));
    }

    /** DELETE /api/notes/{id}/share/{targetUserId} — revoke access for a specific user. */
    @DeleteMapping("/{id}/share/{targetUserId}")
    public ResponseEntity<ApiResponse<Void>> revokeShare(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @PathVariable String targetUserId) {

        noteService.revokeShare(id, user.getId(), targetUserId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** DELETE /api/notes/{id} — soft-delete a note (only owner can delete). */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {

        noteService.softDelete(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
