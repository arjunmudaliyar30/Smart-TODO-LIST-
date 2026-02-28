package com.yourapp.service;

import com.yourapp.dto.UserFitnessProfileRequest;
import com.yourapp.model.UserFitnessProfile;
import com.yourapp.model.UserFitnessProfile.ActivityLevel;
import com.yourapp.repository.UserFitnessProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class UserFitnessProfileService {

    private final UserFitnessProfileRepository profileRepo;

    public UserFitnessProfile getProfile(String userId) {
        return profileRepo.findByUserId(userId).orElse(null);
    }

    /**
     * Upsert — creates or updates the profile for the given user.
     * If dailyCalorieGoal == 0, auto-computes via Mifflin-St Jeor formula.
     */
    public UserFitnessProfile upsertProfile(String userId, UserFitnessProfileRequest req) {
        Optional<UserFitnessProfile> existing = profileRepo.findByUserId(userId);

        int goalKcal = req.getDailyCalorieGoal();
        if (goalKcal <= 0 && req.getAge() > 0 && req.getWeightKg() > 0 && req.getHeightCm() > 0) {
            goalKcal = computeCalorieGoal(req.getWeightKg(), req.getHeightCm(),
                    req.getAge(), req.getGender(), req.getActivityLevel());
        }

        if (existing.isPresent()) {
            UserFitnessProfile profile = existing.get();
            profile.setAge(req.getAge());
            profile.setWeightKg(req.getWeightKg());
            profile.setHeightCm(req.getHeightCm());
            profile.setGender(req.getGender());
            profile.setActivityLevel(req.getActivityLevel());
            if (goalKcal > 0) profile.setDailyCalorieGoal(goalKcal);
            profile.setUpdatedAt(LocalDateTime.now());
            return profileRepo.save(profile);
        }

        UserFitnessProfile profile = UserFitnessProfile.builder()
                .userId(userId)
                .age(req.getAge())
                .weightKg(req.getWeightKg())
                .heightCm(req.getHeightCm())
                .gender(req.getGender())
                .activityLevel(req.getActivityLevel())
                .dailyCalorieGoal(goalKcal)
                .build();
        return profileRepo.save(profile);
    }

    /**
     * Mifflin-St Jeor Formula:
     *   Men:   BMR = 10w + 6.25h − 5a + 5
     *   Women: BMR = 10w + 6.25h − 5a − 161
     * Multiply by activity multiplier to get TDEE.
     */
    public static int computeCalorieGoal(double weightKg, double heightCm,
                                          int age, String gender, ActivityLevel level) {
        double bmr = 10 * weightKg + 6.25 * heightCm - 5 * age;
        bmr += ("M".equalsIgnoreCase(gender)) ? 5 : -161;

        double multiplier = switch (level != null ? level : ActivityLevel.SEDENTARY) {
            case SEDENTARY  -> 1.2;
            case LIGHT      -> 1.375;
            case MODERATE   -> 1.55;
            case ACTIVE     -> 1.725;
            case VERY_ACTIVE-> 1.9;
        };
        return (int) Math.round(bmr * multiplier);
    }
}
