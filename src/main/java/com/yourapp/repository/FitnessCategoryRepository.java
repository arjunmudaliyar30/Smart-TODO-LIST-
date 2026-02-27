package com.yourapp.repository;

import com.yourapp.model.FitnessCategory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FitnessCategoryRepository extends MongoRepository<FitnessCategory, String> {

    /** Returns system defaults (userId=null) AND categories owned by the given user. */
    @Query("{ '$or': [ { 'userId': null }, { 'userId': ?0 } ] }")
    List<FitnessCategory> findSystemAndUserCategories(String userId);

    Optional<FitnessCategory> findByNameAndUserIdIsNull(String name);

    boolean existsByNameAndUserIdIsNull(String name);

    boolean existsByNameAndUserId(String name, String userId);

    List<FitnessCategory> findByUserId(String userId);
}
