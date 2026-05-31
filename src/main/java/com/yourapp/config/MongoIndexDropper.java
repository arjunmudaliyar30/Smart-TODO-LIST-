package com.yourapp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * Drops legacy unique index on daily_notes(userId, date) so that multiple
 * notes per day can be saved. Runs once at startup; safe to leave in place.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MongoIndexDropper implements ApplicationRunner {

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            mongoTemplate.indexOps("daily_notes").dropIndex("user_date_unique");
            log.info("Dropped legacy unique index 'user_date_unique' from daily_notes");
        } catch (Exception ex) {
            // Index may not exist — that is fine
            log.debug("Could not drop 'user_date_unique' (may already be gone): {}", ex.getMessage());
        }
    }
}
