package com.yourapp.repository;

import com.yourapp.model.UserFitnessProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserFitnessProfileRepository extends MongoRepository<UserFitnessProfile, String> {

    Optional<UserFitnessProfile> findByUserId(String userId);
}
