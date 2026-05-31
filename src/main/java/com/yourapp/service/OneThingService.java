package com.yourapp.service;

import com.yourapp.model.OneThing;
import com.yourapp.repository.OneThingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OneThingService {

    private final OneThingRepository oneThingRepository;

    public OneThing setTodayOneThing(String userId, String taskText) {
        LocalDate today = LocalDate.now();
        OneThing entry = oneThingRepository.findByUserIdAndDate(userId, today)
                .orElseGet(() -> OneThing.builder()
                        .userId(userId)
                        .date(today)
                        .build());
        entry.setTaskText(taskText);
        entry.setCompleted(false);
        return oneThingRepository.save(entry);
    }

    public Optional<OneThing> getTodayOneThing(String userId) {
        return oneThingRepository.findByUserIdAndDate(userId, LocalDate.now());
    }

    public OneThing completeToday(String userId) {
        LocalDate today = LocalDate.now();
        OneThing entry = oneThingRepository.findByUserIdAndDate(userId, today)
                .orElseThrow(() -> new IllegalStateException("No 'One Thing' set for today"));
        entry.setCompleted(true);
        return oneThingRepository.save(entry);
    }
}
