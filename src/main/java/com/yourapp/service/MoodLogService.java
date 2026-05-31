package com.yourapp.service;

import com.yourapp.model.MoodLog;
import com.yourapp.repository.MoodLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoodLogService {

    private final MoodLogRepository moodLogRepository;

    /** Upsert — one log per user per day */
    public MoodLog save(String userId, int energy, int mood, int focus, String voiceNote) {
        LocalDate today = LocalDate.now();
        MoodLog entry = moodLogRepository.findByUserIdAndDate(userId, today)
                .orElseGet(() -> MoodLog.builder()
                        .userId(userId)
                        .date(today)
                        .build());
        entry.setEnergy(energy);
        entry.setMood(mood);
        entry.setFocus(focus);
        if (voiceNote != null) entry.setVoiceNote(voiceNote);
        return moodLogRepository.save(entry);
    }

    public Optional<MoodLog> getToday(String userId) {
        return moodLogRepository.findByUserIdAndDate(userId, LocalDate.now());
    }

    public List<MoodLog> getWeekly(String userId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        return moodLogRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, start, end);
    }

    /** Returns avg mood, energy, focus per day-of-week over last 28 days */
    public Map<String, Object> getPatterns(String userId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(27);
        List<MoodLog> logs = moodLogRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, start, end);

        Map<String, Double> avgMood = logs.stream()
                .collect(Collectors.groupingBy(
                        ml -> ml.getDate().getDayOfWeek().toString(),
                        Collectors.averagingInt(MoodLog::getMood)));
        Map<String, Double> avgEnergy = logs.stream()
                .collect(Collectors.groupingBy(
                        ml -> ml.getDate().getDayOfWeek().toString(),
                        Collectors.averagingInt(MoodLog::getEnergy)));
        Map<String, Double> avgFocus = logs.stream()
                .collect(Collectors.groupingBy(
                        ml -> ml.getDate().getDayOfWeek().toString(),
                        Collectors.averagingInt(MoodLog::getFocus)));

        return Map.of(
                "avgMoodByDay", avgMood,
                "avgEnergyByDay", avgEnergy,
                "avgFocusByDay", avgFocus,
                "totalEntries", logs.size()
        );
    }
}
