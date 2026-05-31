package com.yourapp.service;

import com.yourapp.model.Alarm;
import com.yourapp.repository.AlarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AlarmService {

    private final AlarmRepository alarmRepository;

    public Alarm create(String userId, String title, LocalDateTime scheduledAt) {
        Alarm alarm = Alarm.builder()
                .userId(userId)
                .title(title)
                .scheduledAt(scheduledAt)
                .build();
        return alarmRepository.save(alarm);
    }

    public List<Alarm> listForUser(String userId) {
        return alarmRepository.findByUserIdOrderByScheduledAtAsc(userId);
    }

    /** Returns alarms that are due now for the user and marks them fired. */
    public List<Alarm> popPendingForUser(String userId) {
        List<Alarm> pending = alarmRepository.findPendingForUser(userId, LocalDateTime.now());
        pending.forEach(a -> {
            a.setFired(true);
            alarmRepository.save(a);
        });
        return pending;
    }

    public void dismiss(String userId, String alarmId) {
        alarmRepository.findById(alarmId).ifPresent(a -> {
            if (a.getUserId().equals(userId)) {
                a.setDismissed(true);
                alarmRepository.save(a);
            }
        });
    }

    public void delete(String userId, String alarmId) {
        alarmRepository.findById(alarmId).ifPresent(a -> {
            if (a.getUserId().equals(userId)) {
                alarmRepository.delete(a);
            }
        });
    }
}
