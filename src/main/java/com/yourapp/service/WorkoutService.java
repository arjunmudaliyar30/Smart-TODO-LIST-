package com.yourapp.service;

import com.yourapp.dto.ExerciseRequest;
import com.yourapp.dto.WorkoutRequest;
import com.yourapp.event.WorkoutCompletedEvent;
import com.yourapp.model.Workout;
import com.yourapp.model.Workout.Exercise;
import com.yourapp.model.Workout.ExerciseStatus;
import com.yourapp.model.Workout.WorkoutStatus;
import com.yourapp.model.Workout.WorkoutType;
import com.yourapp.repository.UserRepository;
import com.yourapp.repository.WorkoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class WorkoutService {

    private final WorkoutRepository       workoutRepository;
    private final UserRepository           userRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ---- CRUD ----

    @SuppressWarnings("null")
    public Workout createWorkout(String userId, WorkoutRequest request) {
        Workout workout = Workout.builder()
                .userId(userId)
                .name(request.getName())
                .type(request.getType())
                .workoutDate(request.getWorkoutDate() != null ? request.getWorkoutDate() : LocalDate.now())
                .durationMinutes(request.getDurationMinutes())
                .caloriesBurned(request.getCaloriesBurned())
                .status(request.getStatus() != null ? request.getStatus() : WorkoutStatus.PENDING)
                .linkedGoalId(request.getLinkedGoalId())
                .collaboratorIds(request.getCollaboratorIds() != null ? request.getCollaboratorIds() : new ArrayList<>())
                .exercises(request.getExercises() != null ? request.getExercises() : new ArrayList<>())
                .notes(request.getNotes())
                .build();
        return workoutRepository.save(workout);
    }

    /** Alias kept for AI service compatibility */
    public Workout logWorkout(String userId, WorkoutRequest request) {
        return createWorkout(userId, request);
    }

    public List<Workout> getActiveWorkouts(String userId) {
        return workoutRepository.findByUserIdAndArchivedFalseOrderByCreatedAtDesc(userId);
    }

    public List<Workout> getArchivedWorkouts(String userId) {
        return workoutRepository.findByUserIdAndArchivedTrueOrderByCreatedAtDesc(userId);
    }

    public List<Workout> getUserWorkouts(String userId) {
        List<Workout> owned  = workoutRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<Workout> shared = workoutRepository.findByCollaboratorIdsContaining(userId);
        Set<String>   seen   = new HashSet<>();
        List<Workout> merged = new ArrayList<>(owned);
        owned.forEach(w  -> seen.add(w.getId()));
        shared.forEach(w -> { if (seen.add(w.getId())) merged.add(w); });
        return merged;
    }

    public List<Workout> getWorkoutsByType(String userId, WorkoutType type) {
        return workoutRepository.findByUserIdAndType(userId, type);
    }

    public List<Workout> getWorkoutsInRange(String userId, LocalDate start, LocalDate end) {
        return workoutRepository.findByUserIdAndWorkoutDateBetween(userId, start, end);
    }

    public Workout getWorkoutById(String userId, String workoutId) {
        return workoutRepository.findByIdAndUserId(workoutId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Workout not found"));
    }

    public Workout updateWorkout(String userId, String workoutId, WorkoutRequest request) {
        Workout workout = getWorkoutById(userId, workoutId);

        workout.setName(request.getName());
        if (request.getType() != null) workout.setType(request.getType());
        if (request.getWorkoutDate() != null) workout.setWorkoutDate(request.getWorkoutDate());
        workout.setDurationMinutes(request.getDurationMinutes());
        workout.setCaloriesBurned(request.getCaloriesBurned());
        if (request.getLinkedGoalId() != null) workout.setLinkedGoalId(request.getLinkedGoalId());
        if (request.getNotes() != null) workout.setNotes(request.getNotes());
        if (request.getExercises() != null) workout.setExercises(request.getExercises());

        return workoutRepository.save(workout);
    }

    public void deleteWorkout(String userId, String workoutId) {
        getWorkoutById(userId, workoutId);
        workoutRepository.deleteByIdAndUserId(workoutId, userId);
    }

    // ---- STATUS ----

    public Workout updateStatus(String userId, String workoutId, WorkoutStatus status) {
        Workout workout = getWorkoutById(userId, workoutId);
        workout.setStatus(status);
        Workout saved = workoutRepository.save(workout);
        if (status == WorkoutStatus.COMPLETED) {
            java.time.LocalDate date = saved.getWorkoutDate() != null
                    ? saved.getWorkoutDate() : java.time.LocalDate.now();
            eventPublisher.publishEvent(new WorkoutCompletedEvent(this, saved.getUserId(), saved.getId(), date));
        }
        return saved;
    }

    // ---- ARCHIVE ----

    public Workout toggleArchive(String userId, String workoutId) {
        Workout workout = getWorkoutById(userId, workoutId);
        workout.setArchived(!workout.isArchived());
        if (workout.isArchived()) workout.setStatus(WorkoutStatus.ARCHIVED);
        return workoutRepository.save(workout);
    }

    // ---- COLLABORATORS ----

    public Workout addCollaborator(String userId, String workoutId, String collaboratorEmail) {
        Workout workout = getWorkoutById(userId, workoutId);
        var collaborator = userRepository.findByEmail(collaboratorEmail)
                .orElseThrow(() -> new IllegalArgumentException("User with email '" + collaboratorEmail + "' not found"));
        String collabId = collaborator.getId();
        if (!workout.getCollaboratorIds().contains(collabId)) {
            workout.getCollaboratorIds().add(collabId);
        }
        return workoutRepository.save(workout);
    }

    public Workout removeCollaborator(String userId, String workoutId, String collaboratorId) {
        Workout workout = getWorkoutById(userId, workoutId);
        workout.getCollaboratorIds().remove(collaboratorId);
        return workoutRepository.save(workout);
    }

    // ---- EXERCISES ----

    public Workout addExercise(String userId, String workoutId, ExerciseRequest req) {
        Workout workout = getWorkoutById(userId, workoutId);
        Exercise ex = Exercise.builder()
                .name(req.getName())
                .sets(req.getSets())
                .reps(req.getReps())
                .weightKg(req.getWeightKg())
                .durationSeconds(req.getDurationSeconds())
                .status(req.getStatus() != null ? req.getStatus() : ExerciseStatus.PENDING)
                .notes(req.getNotes())
                .build();
        if (workout.getExercises() == null) workout.setExercises(new ArrayList<>());
        workout.getExercises().add(ex);

        // If first exercise added, move workout from DRAFT to PENDING
        if (workout.getStatus() == WorkoutStatus.DRAFT) {
            workout.setStatus(WorkoutStatus.PENDING);
        }
        return workoutRepository.save(workout);
    }

    public Workout updateExercise(String userId, String workoutId, int index, ExerciseRequest req) {
        Workout workout = getWorkoutById(userId, workoutId);
        List<Exercise> exercises = workout.getExercises();
        if (index < 0 || index >= exercises.size()) {
            throw new IllegalArgumentException("Exercise index out of range");
        }
        Exercise ex = exercises.get(index);
        if (req.getName() != null) ex.setName(req.getName());
        ex.setSets(req.getSets());
        ex.setReps(req.getReps());
        ex.setWeightKg(req.getWeightKg());
        if (req.getDurationSeconds() > 0) ex.setDurationSeconds(req.getDurationSeconds());
        if (req.getNotes() != null) ex.setNotes(req.getNotes());
        if (req.getStatus() != null) ex.setStatus(req.getStatus());
        exercises.set(index, ex);

        checkAutoComplete(workout);
        return workoutRepository.save(workout);
    }

    public Workout updateExerciseStatus(String userId, String workoutId, int index, ExerciseStatus status) {
        Workout workout = getWorkoutById(userId, workoutId);
        List<Exercise> exercises = workout.getExercises();
        if (index < 0 || index >= exercises.size()) {
            throw new IllegalArgumentException("Exercise index out of range");
        }
        exercises.get(index).setStatus(status);

        // Move workout to IN_PROGRESS if any exercise is in progress / done
        if (workout.getStatus() == WorkoutStatus.PENDING || workout.getStatus() == WorkoutStatus.DRAFT) {
            if (status == ExerciseStatus.IN_PROGRESS || status == ExerciseStatus.DONE) {
                workout.setStatus(WorkoutStatus.IN_PROGRESS);
            }
        }

        checkAutoComplete(workout);
        return workoutRepository.save(workout);
    }

    public Workout deleteExercise(String userId, String workoutId, int index) {
        Workout workout = getWorkoutById(userId, workoutId);
        List<Exercise> exercises = workout.getExercises();
        if (index < 0 || index >= exercises.size()) {
            throw new IllegalArgumentException("Exercise index out of range");
        }
        exercises.remove(index);
        checkAutoComplete(workout);
        return workoutRepository.save(workout);
    }

    // ---- COLLABORATOR ACCESS (allow collabs to update exercise status) ----

    public Workout updateExerciseStatusByCollaborator(String actorUserId, String workoutId, int index, ExerciseStatus status) {
        Workout workout = workoutRepository.findById(workoutId)
                .orElseThrow(() -> new IllegalArgumentException("Workout not found"));
        boolean isOwner = workout.getUserId().equals(actorUserId);
        boolean isCollab = workout.getCollaboratorIds().contains(actorUserId);
        if (!isOwner && !isCollab) {
            throw new SecurityException("Access denied");
        }
        List<Exercise> exercises = workout.getExercises();
        if (index < 0 || index >= exercises.size()) {
            throw new IllegalArgumentException("Exercise index out of range");
        }
        exercises.get(index).setStatus(status);
        if (workout.getStatus() == WorkoutStatus.PENDING || workout.getStatus() == WorkoutStatus.DRAFT) {
            if (status == ExerciseStatus.IN_PROGRESS || status == ExerciseStatus.DONE) {
                workout.setStatus(WorkoutStatus.IN_PROGRESS);
            }
        }
        checkAutoComplete(workout);
        return workoutRepository.save(workout);
    }

    // ---- PRIVATE ----

    /** If all exercises are DONE, auto-complete the workout */
    private void checkAutoComplete(Workout workout) {
        List<Exercise> exercises = workout.getExercises();
        if (exercises != null && !exercises.isEmpty()
                && exercises.stream().allMatch(e -> ExerciseStatus.DONE.equals(e.getStatus()))) {
            workout.setStatus(WorkoutStatus.COMPLETED);
            java.time.LocalDate date = workout.getWorkoutDate() != null
                    ? workout.getWorkoutDate() : java.time.LocalDate.now();
            eventPublisher.publishEvent(new WorkoutCompletedEvent(this, workout.getUserId(), workout.getId(), date));
        }
    }
}

