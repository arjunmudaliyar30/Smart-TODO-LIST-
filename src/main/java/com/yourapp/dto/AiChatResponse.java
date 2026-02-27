package com.yourapp.dto;

import com.yourapp.model.ChatMessage;
import com.yourapp.model.Goal;
import com.yourapp.model.Task;
import com.yourapp.model.Workout;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned by POST /api/ai/chat.
 * When the AI detects a schedule/create-task intent, taskCreated will be non-null
 * and the frontend can immediately refresh the task list.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {

    /** The saved assistant ChatMessage (contains the displayed reply text). */
    private ChatMessage message;

    /**
     * Non-null when the AI understood a scheduling intent and created a task in MongoDB.
     * Frontend should show a toast and refresh the task panel.
     */
    private Task taskCreated;

    /**
     * Non-null when the AI understood a goal-creation intent and created a goal in MongoDB.
     * Frontend should show a toast and refresh the goals panel.
     */
    private Goal goalCreated;

    /**
     * Non-null when the AI understood a workout-logging intent and created a workout in MongoDB.
     * Frontend should show a toast and refresh the workouts panel.
     */
    private Workout workoutCreated;
}
