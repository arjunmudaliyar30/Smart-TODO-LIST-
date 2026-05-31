package com.yourapp.repository;

import com.yourapp.model.Partnership;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PartnershipRepository extends MongoRepository<Partnership, String> {

    @Query("{ '$or': [ { 'userId1': ?0 }, { 'userId2': ?0 } ] }")
    List<Partnership> findByUserId(String userId);

    @Query("{ '$or': [ { 'userId1': ?0, 'userId2': ?1 }, { 'userId1': ?1, 'userId2': ?0 } ] }")
    Optional<Partnership> findByUserPair(String userId1, String userId2);

    @Query("{ '$or': [ { 'userId1': ?0, 'status': 'active' }, { 'userId2': ?0, 'status': 'active' } ] }")
    Optional<Partnership> findActiveByUserId(String userId);
}
