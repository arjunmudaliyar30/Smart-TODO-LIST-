package com.yourapp.controller;

import com.yourapp.dto.ApiResponse;
import com.yourapp.model.FileMetadata;
import com.yourapp.model.User;
import com.yourapp.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileMetadata>> uploadFile(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String taskId,
            @RequestParam(required = false) String description) throws IOException {

        FileMetadata metadata = fileService.uploadFile(user.getId(), file, taskId, description);
        return ResponseEntity.ok(ApiResponse.success("File uploaded", metadata));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FileMetadata>>> getFiles(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String taskId) {

        List<FileMetadata> files = taskId != null
                ? fileService.getFilesByTask(user.getId(), taskId)
                : fileService.getUserFiles(user.getId());
        return ResponseEntity.ok(ApiResponse.success(files));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(
            @AuthenticationPrincipal User user,
            @PathVariable String id) throws MalformedURLException {

        FileMetadata metadata = fileService.getFileById(user.getId(), id);
        Path filePath = Paths.get(metadata.getFilePath());
        Resource resource = new UrlResource(filePath.toUri());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        metadata.getContentType() != null ? metadata.getContentType() : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + metadata.getOriginalFilename() + "\"")
                .body(resource);
    }

    /**
     * GET /api/files/{id}/preview
     * Returns the file inline (for browser display) instead of forcing a download.
     * Supports images, PDFs, and plain text. Other types are also returned inline
     * so the browser can decide whether to display or prompt.
     */
    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> previewFile(
            @AuthenticationPrincipal User user,
            @PathVariable String id) throws MalformedURLException {

        FileMetadata metadata = fileService.getFileById(user.getId(), id);
        Path filePath = Paths.get(metadata.getFilePath());
        Resource resource = new UrlResource(filePath.toUri());

        String contentType = metadata.getContentType() != null
                ? metadata.getContentType()
                : "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + metadata.getOriginalFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @AuthenticationPrincipal User user,
            @PathVariable String id) throws IOException {

        fileService.deleteFile(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("File deleted", null));
    }
}
