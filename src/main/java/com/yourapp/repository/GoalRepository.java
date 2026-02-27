package com.yourapp.repository;

import com.yourapp.model.Goal;
import com.yourapp.model.Goal.GoalCategory;
import com.yourapp.model.Goal.GoalStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoalRepository extends MongoRepository<Goal, String> {

    List<Goal> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Goal> findByUserIdAndStatus(String userId, GoalStatus status);

    List<Goal> findByUserIdAndCategory(String userId, GoalCategory category);

    Optional<Goal> findByIdAndUserId(String id, String userId);

    void deleteByIdAndUserId(String id, String userId);

    /** Goals shared with the given user as a collaborator. */
    List<Goal> findByCollaboratorIdsContaining(String userId);
}
