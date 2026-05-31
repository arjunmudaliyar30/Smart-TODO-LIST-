package com.yourapp.repository;

import com.yourapp.model.Session;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SessionRepository extends MongoRepository<Session, String> {

    List<Session> findByUserIdOrderBySessionDateDescCreatedAtDesc(String userId);
}
