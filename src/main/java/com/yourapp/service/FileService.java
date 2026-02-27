package com.yourapp.service;

import com.yourapp.model.FileMetadata;
import com.yourapp.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final FileMetadataRepository fileMetadataRepository;

    @Value("${file.upload.dir:uploads/}")
    private String uploadDir;

    /** Comma-separated allowed MIME types; configured in application.properties */
    @Value("${file.allowed-types:image/jpeg,image/png,image/gif,image/webp,application/pdf,text/plain,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document}")
    private String allowedTypesRaw;

    @Value("${file.max-size-mb:10}")
    private long maxFileSizeMb;

    public FileMetadata uploadFile(String userId, MultipartFile file, String taskId, String description)
            throws IOException {

        // --- Content-type validation ---
        String contentType = file.getContentType();
        List<String> allowed = Arrays.stream(allowedTypesRaw.split(","))
                .map(String::trim)
                .toList();

        if (contentType == null || !allowed.contains(contentType)) {
            log.warn("FILE_REJECTED userId={} contentType={} filename={}",
                    userId, contentType, file.getOriginalFilename());
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "File type '" + contentType + "' is not allowed. Accepted: " + allowedTypesRaw
            );
        }

        // --- Size validation ---
        long maxBytes = maxFileSizeMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "File exceeds maximum allowed size of " + maxFileSizeMb + " MB"
            );
        }

        // Ensure upload directory exists
        Path uploadPath = Paths.get(uploadDir, userId);
        Files.createDirectories(uploadPath);

        // Build a unique filename
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        String storedFilename = UUID.randomUUID() + (extension.isBlank() ? "" : "." + extension);

        Path filePath = uploadPath.resolve(storedFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("FILE_UPLOADED userId={} filename={} type={} sizeKb={}",
                userId, file.getOriginalFilename(), contentType, file.getSize() / 1024);

        FileMetadata metadata = FileMetadata.builder()
                .userId(userId)
                .originalFilename(file.getOriginalFilename())
                .storedFilename(storedFilename)
                .filePath(filePath.toString())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .taskId(taskId)
                .description(description)
                .build();

        return fileMetadataRepository.save(metadata);
    }

    public List<FileMetadata> getUserFiles(String userId) {
        List<FileMetadata> owned  = fileMetadataRepository.findByUserIdOrderByUploadedAtDesc(userId);
        List<FileMetadata> shared = fileMetadataRepository.findByCollaboratorIdsContaining(userId);
        // Merge, dedup by ID
        java.util.Set<String>       seen   = new java.util.HashSet<>();
        java.util.List<FileMetadata> merged = new java.util.ArrayList<>(owned);
        owned.forEach(f  -> seen.add(f.getId()));
        shared.forEach(f -> { if (seen.add(f.getId())) merged.add(f); });
        return merged;
    }

    public List<FileMetadata> getFilesByTask(String userId, String taskId) {
        return fileMetadataRepository.findByUserIdAndTaskId(userId, taskId);
    }

    public FileMetadata getFileById(String userId, String fileId) {
        // Check if the user is the owner OR a listed collaborator
        return fileMetadataRepository.findById(fileId)
                .filter(f -> userId.equals(f.getUserId())
                        || (f.getCollaboratorIds() != null && f.getCollaboratorIds().contains(userId)))
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
    }

    public void deleteFile(String userId, String fileId) throws IOException {
        FileMetadata metadata = getFileById(userId, fileId);
        Path path = Paths.get(metadata.getFilePath());
        Files.deleteIfExists(path);
        fileMetadataRepository.deleteByIdAndUserId(fileId, userId);
    }
}
