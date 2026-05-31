package com.yourapp.service;

import com.yourapp.model.Session;
import com.yourapp.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class SessionService {

    private final SessionRepository sessionRepository;

    public Session create(Session session) {
        return sessionRepository.save(session);
    }

    public List<Session> listForUser(String userId) {
        return sessionRepository.findByUserIdOrderBySessionDateDescCreatedAtDesc(userId);
    }

    public void delete(String userId, String id) {
        sessionRepository.findById(id).ifPresent(s -> {
            if (s.getUserId().equals(userId)) sessionRepository.delete(s);
        });
    }
}
