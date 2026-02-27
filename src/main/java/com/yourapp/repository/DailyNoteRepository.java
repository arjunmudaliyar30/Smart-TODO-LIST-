package com.yourapp.repository;

import com.yourapp.model.DailyNote;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyNoteRepository extends MongoRepository<DailyNote, String> {

    Optional<DailyNote> findByUserIdAndDateAndDeletedFalse(String userId, LocalDate date);

    List<DailyNote> findByUserIdAndDeletedFalseAndDateBetweenOrderByDateDesc(
            String userId, LocalDate start, LocalDate end);

    /** Keyword search in content (case-insensitive). */
    @Query("{ 'userId': ?0, 'deleted': false, 'content': { $regex: ?1, $options: 'i' } }")
    List<DailyNote> searchByContent(String userId, String keyword);

    /** Notes shared with a specific user (via share records whose noteIds we pass in). */
    List<DailyNote> findByIdInAndDeletedFalse(List<String> ids);

    boolean existsByUserIdAndDate(String userId, LocalDate date);
}
