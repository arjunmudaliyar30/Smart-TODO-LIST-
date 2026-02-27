package com.yourapp.repository;

import com.yourapp.model.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    List<ChatMessage> findByUserIdOrderByTimestampAsc(String userId);

    List<ChatMessage> findByUserIdAndSessionIdOrderByTimestampAsc(String userId, String sessionId);

    // Last N messages for context window (use findTop20... for efficiency)
    List<ChatMessage> findTop20ByUserIdAndSessionIdOrderByTimestampDesc(String userId, String sessionId);

    void deleteByUserIdAndSessionId(String userId, String sessionId);
}
