package com.yourapp.controller;

import com.yourapp.dto.ApiResponse;
import com.yourapp.model.FileMetadata;
import com.yourapp.model.ProgressPhoto;
import com.yourapp.model.User;
import com.yourapp.repository.ProgressPhotoRepository;
import com.yourapp.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@SuppressWarnings("null")
@RestController
@RequestMapping("/api/progress-photos")
@RequiredArgsConstructor
public class ProgressPhotoController {

    private final ProgressPhotoRepository progressPhotoRepository;
    private final FileService fileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProgressPhoto>> upload(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) String photoDate) throws IOException {

        FileMetadata meta = fileService.uploadFile(user.getId(), file, null, "progress-photo");

        ProgressPhoto photo = ProgressPhoto.builder()
                .userId(user.getId())
                .fileId(meta.getId())
                .previewUrl("/api/files/" + meta.getId() + "/preview")
                .notes(notes)
                .photoDate(photoDate != null ? LocalDate.parse(photoDate) : LocalDate.now())
                .build();

        return ResponseEntity.ok(ApiResponse.success(progressPhotoRepository.save(photo)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProgressPhoto>>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(
                progressPhotoRepository.findByUserIdOrderByPhotoDateDescCreatedAtDesc(user.getId())));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ProgressPhoto>> updateNotes(
            @AuthenticationPrincipal User user,
            @PathVariable String id,
            @RequestBody Map<String, String> body) {

        return progressPhotoRepository.findById(id)
                .filter(p -> p.getUserId().equals(user.getId()))
                .map(p -> {
                    if (body.containsKey("notes")) p.setNotes(body.get("notes"));
                    return ResponseEntity.ok(ApiResponse.success(progressPhotoRepository.save(p)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {

        progressPhotoRepository.findById(id)
                .filter(p -> p.getUserId().equals(user.getId()))
                .ifPresent(progressPhotoRepository::delete);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }
}
