package com.yourapp.repository;

import com.yourapp.model.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {

    /** All messages between two users (both directions) */
    List<Message> findBySenderIdAndRecipientIdOrSenderIdAndRecipientIdOrderByCreatedAtAsc(
            String s1, String r1, String s2, String r2);

    /** All messages where current user is sender or recipient */
    List<Message> findBySenderIdOrRecipientIdOrderByCreatedAtDesc(
            String senderId, String recipientId);

    /** Unread messages for a recipient */
    List<Message> findByRecipientIdAndReadFalse(String recipientId);

    long countByRecipientIdAndReadFalse(String recipientId);
}
