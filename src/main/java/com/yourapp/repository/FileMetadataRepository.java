package com.yourapp.repository;

import com.yourapp.model.FileMetadata;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileMetadataRepository extends MongoRepository<FileMetadata, String> {

    List<FileMetadata> findByUserIdOrderByUploadedAtDesc(String userId);

    List<FileMetadata> findByUserIdAndTaskId(String userId, String taskId);

    Optional<FileMetadata> findByIdAndUserId(String id, String userId);

    void deleteByIdAndUserId(String id, String userId);

    /** Files shared with the given user as a collaborator. */
    List<FileMetadata> findByCollaboratorIdsContaining(String userId);
}
