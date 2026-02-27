package com.yourapp.repository;

import com.yourapp.model.DailyNoteShare;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DailyNoteShareRepository extends MongoRepository<DailyNoteShare, String> {

    List<DailyNoteShare> findByNoteId(String noteId);

    Optional<DailyNoteShare> findByNoteIdAndSharedWithUserId(String noteId, String sharedWithUserId);

    List<DailyNoteShare> findBySharedWithUserId(String sharedWithUserId);

    /** All note IDs visible to a given user (as collaborator). */
    List<DailyNoteShare> findBySharedWithUserIdAndNoteIdIn(String userId, List<String> noteIds);

    void deleteByNoteIdAndSharedWithUserId(String noteId, String sharedWithUserId);

    void deleteByNoteId(String noteId);
}
