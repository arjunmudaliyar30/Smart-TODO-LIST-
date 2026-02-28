package com.yourapp.service;

import com.yourapp.dto.CaloriesLogRequest;
import com.yourapp.event.CaloriesLoggedEvent;
import com.yourapp.model.CaloriesLog;
import com.yourapp.repository.CaloriesLogRepository;
import com.yourapp.repository.UserFitnessProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CaloriesLogService {

    private final CaloriesLogRepository        repo;
    private final UserFitnessProfileRepository userFitnessProfileRepository;
    private final ApplicationEventPublisher    eventPublisher;

    @SuppressWarnings("null")
    public CaloriesLog create(String userId, CaloriesLogRequest req) {
        CaloriesLog log = CaloriesLog.builder()
                .userId(userId)
                .date(req.getDate() != null ? req.getDate() : LocalDate.now())
                .consumed(req.getConsumed())
                .burned(req.getBurned())
                .mealType(req.getMealType())
                .note(req.getNote())
                .build();
        CaloriesLog saved = repo.save(log);
        fireCalorieEvent(userId, saved.getDate());
        return saved;
    }

    public List<CaloriesLog> getByDate(String userId, LocalDate date) {
        return repo.findByUserIdAndDateOrderByCreatedAtAsc(userId, date);
    }

    public List<CaloriesLog> getAll(String userId) {
        return repo.findByUserIdOrderByDateDesc(userId);
    }

    public CaloriesLog update(String userId, String id, CaloriesLogRequest req) {
        CaloriesLog log = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Log entry not found"));
        log.setConsumed(req.getConsumed());
        log.setBurned(req.getBurned());
        if (req.getMealType() != null) log.setMealType(req.getMealType());
        if (req.getNote()     != null) log.setNote(req.getNote());
        if (req.getDate()     != null) log.setDate(req.getDate());
        CaloriesLog saved = repo.save(log);
        fireCalorieEvent(userId, saved.getDate());
        return saved;
    }

    public void delete(String userId, String id) {
        repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Log entry not found"));
        repo.deleteByIdAndUserId(id, userId);
    }

    /**
     * Returns { consumed: int, burned: int } totals for a date.
     */
    public Map<String, Integer> getDailySummary(String userId, LocalDate date) {
        List<CaloriesLog> logs = getByDate(userId, date);
        int consumed = logs.stream().mapToInt(CaloriesLog::getConsumed).sum();
        int burned   = logs.stream().mapToInt(CaloriesLog::getBurned).sum();
        return Map.of("consumed", consumed, "burned", burned, "net", consumed - burned);
    }

    // --- Private ---

    private void fireCalorieEvent(String userId, LocalDate date) {
        List<CaloriesLog> logs = getByDate(userId, date);
        int totalConsumed = logs.stream().mapToInt(CaloriesLog::getConsumed).sum();
        int calorieGoal   = userFitnessProfileRepository.findByUserId(userId)
                .map(p -> p.getDailyCalorieGoal()).orElse(0);
        eventPublisher.publishEvent(new CaloriesLoggedEvent(this, userId, date, totalConsumed, calorieGoal));
    }
}
