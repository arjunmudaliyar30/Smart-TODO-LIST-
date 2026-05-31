package com.yourapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yourapp.config.OpenAIConfig;
import com.yourapp.dto.AiChatRequest;
import com.yourapp.dto.AiChatResponse;
import com.yourapp.model.ChatMessage;
import com.yourapp.model.ChatMessage.MessageRole;
import com.yourapp.model.Goal;
import com.yourapp.model.Task;
import com.yourapp.model.Task.TaskPriority;
import com.yourapp.model.Task.TaskStatus;
import com.yourapp.model.Workout;
import com.yourapp.model.Workout.WorkoutType;
import com.yourapp.repository.ChatMessageRepository;
import com.yourapp.repository.GoalRepository;
import com.yourapp.repository.TaskRepository;
import com.yourapp.repository.WorkoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"null", "resource"})
public class AiService {

    private final OpenAIConfig openAIConfig;
    private final OkHttpClient okHttpClient;
    private final ChatMessageRepository chatMessageRepository;
    private final TaskRepository taskRepository;
    private final GoalRepository goalRepository;
    private final WorkoutRepository workoutRepository;
    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");

    // -------------------------------------------------------------------------
    // System prompt — strict app-domain lock + JSON action protocol
    // -------------------------------------------------------------------------

    private static final String SYSTEM_PROMPT_TEMPLATE =
        "You are the AI assistant built into \"AI Execution System\" — a personal productivity and accountability platform.\n\n" +
        "YOUR PURPOSE:\n" +
        "- Help users plan, schedule, and complete tasks\n" +
        "- Support goal setting, tracking, and accountability\n" +
        "- Offer productivity coaching, daily planning, prioritisation, and motivation\n" +
        "- Summarise progress, celebrate wins, and highlight pending work\n\n" +
        "STRICT RULES:\n" +
        "- ONLY respond to topics related to tasks, goals, habits, planning, productivity, focus, and personal development\n" +
        "- NEVER answer programming questions, coding help, general knowledge, science, or anything unrelated to this platform\n" +
        "- If asked off-topic, politely refuse: say you are a productivity coach and redirect to tasks/goals\n\n" +
        "RESPONSE FORMAT — you must ALWAYS respond with valid JSON only, no markdown, no extra text:\n\n" +
        "For normal replies:\n" +
        "{\"action\":\"REPLY\",\"message\":\"<your response>\"}\n\n" +
        "When the user wants to schedule, add, create, or remind about a SINGLE TASK:\n" +
        "{\"action\":\"CREATE_TASK\",\"title\":\"<task title>\",\"description\":\"<description or null>\",\"dueDate\":\"<ISO-8601 datetime e.g. 2026-05-30T09:00:00 or null>\",\"priority\":\"<HIGH|MEDIUM|LOW or null>\",\"category\":\"<category string or null>\",\"message\":\"<friendly confirmation>\"}\n\n" +
        "When the user mentions TWO OR MORE things to schedule, do, or create — even in the same sentence — ALWAYS use CREATE_TASKS:\n" +
        "{\"action\":\"CREATE_TASKS\",\"tasks\":[{\"title\":\"<title>\",\"description\":\"<desc or null>\",\"dueDate\":\"<ISO datetime or null>\",\"priority\":\"<HIGH|MEDIUM|LOW or null>\",\"category\":\"<cat or null>\"},...],\"message\":\"<friendly confirmation listing all task titles>\"}\n\n" +
        "When the user wants to add, set, or save a GOAL (a long-term ambition, dream, aspiration, or life objective):\n" +
        "{\"action\":\"CREATE_GOAL\",\"title\":\"<goal title>\",\"description\":\"<brief description or null>\",\"category\":\"<CAREER|FINANCE|FITNESS|EDUCATION|PERSONAL|BUSINESS|HEALTH|RELATIONSHIP|OTHER>\",\"targetDate\":\"<ISO-8601 date or null>\",\"message\":\"<friendly confirmation>\"}\n\n" +
        "When the user says they ALREADY DID, JUST COMPLETED, or FINISHED a workout/exercise (past tense log):\n" +
        "{\"action\":\"CREATE_WORKOUT\",\"name\":\"<workout name>\",\"type\":\"<STRENGTH|CARDIO|FLEXIBILITY|HIIT|SPORT|OTHER>\",\"workoutDate\":\"<ISO-8601 date or null for today>\",\"durationMinutes\":<number or 0>,\"notes\":\"<notes or null>\",\"message\":\"<friendly confirmation>\"}\n\n" +
        "CRITICAL DISTINCTIONS:\n" +
        "- SCHEDULING FUTURE activity (meetings, runs, classes, exercise plans) → CREATE_TASK or CREATE_TASKS. NOT CREATE_WORKOUT.\n" +
        "- LOGGING PAST activity ('I just ran 5km', 'I completed chest day', 'log my workout') → CREATE_WORKOUT.\n" +
        "- Multiple items mentioned = ALWAYS CREATE_TASKS (not multiple separate responses).\n" +
        "- Goals = life ambitions ('I want to be a millionaire', 'become fit by Dec') → CREATE_GOAL.\n" +
        "- Files must be uploaded via Files tab → REPLY.\n\n" +
        "DATE RESOLUTION — compute exact ISO dates from these relative expressions:\n" +
        "Today=%s | Tomorrow=%s | Day-after-tomorrow=%s\n" +
        "Time expressions: 'morning'=09:00, 'afternoon'=14:00, 'evening'=18:00, 'night'=20:00, 'noon'=12:00.\n" +
        "Always output full ISO-8601 datetime when a time/date is mentioned (e.g. 2026-05-30T09:00:00).\n\n" +
        "USER CONTEXT (use this to give personalised advice):\n" +
        "Pending tasks (%d): %s\n" +
        "Completed today (%d): %s\n" +
        "Active goals (%d): %s\n";

    // -------------------------------------------------------------------------
    // Chat — with task scheduling intent detection
    // -------------------------------------------------------------------------

    public AiChatResponse chat(String userId, AiChatRequest request) throws IOException {
        rateLimitService.checkLimit(userId);

        String sessionId = request.getSessionId() != null
                ? request.getSessionId()
                : UUID.randomUUID().toString();

        // Save user message
        ChatMessage userMsg = ChatMessage.builder()
                .userId(userId)
                .sessionId(sessionId)
                .role(MessageRole.USER)
                .content(request.getMessage())
                .build();
        chatMessageRepository.save(userMsg);

        // Build recent chat history (last 15 messages)
        List<ChatMessage> history = chatMessageRepository
                .findTop20ByUserIdAndSessionIdOrderByTimestampDesc(userId, sessionId);
        Collections.reverse(history);
        // Remove the message we just saved (avoid duplicate in history)
        if (!history.isEmpty() && history.get(history.size() - 1).getRole() == MessageRole.USER) {
            history = history.subList(0, history.size() - 1);
        }

        // Build context-aware system prompt
        String systemPrompt = buildSystemPrompt(userId);

        // Call Groq / LLM
        String rawReply = callLLM(systemPrompt, history, request.getMessage());

        // Parse the JSON action response
        Task createdTask = null;
        List<Task> createdTasks = null;
        Goal createdGoal = null;
        Workout createdWorkout = null;
        String displayMessage;

        try {
            // Strip any accidental markdown code fences
            String cleaned = rawReply.trim()
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();

            JsonNode node = objectMapper.readTree(cleaned);
            String action = node.path("action").asText("REPLY");
            displayMessage = node.path("message").asText(rawReply);

            if ("CREATE_TASK".equals(action)) {
                createdTask = createTaskFromAiAction(userId, node);
                log.info("AI_TASK_CREATED userId={} title={}", userId, createdTask.getTitle());
            } else if ("CREATE_TASKS".equals(action)) {
                JsonNode tasksNode = node.path("tasks");
                if (tasksNode.isArray() && !tasksNode.isEmpty()) {
                    createdTasks = new ArrayList<>();
                    for (JsonNode taskNode : tasksNode) {
                        Task t = createTaskFromAiAction(userId, taskNode);
                        createdTasks.add(t);
                        log.info("AI_TASK_CREATED userId={} title={}", userId, t.getTitle());
                    }
                }
            } else if ("CREATE_GOAL".equals(action)) {
                createdGoal = createGoalFromAiAction(userId, node);
                log.info("AI_GOAL_CREATED userId={} title={}", userId, createdGoal.getTitle());
            } else if ("CREATE_WORKOUT".equals(action)) {
                createdWorkout = createWorkoutFromAiAction(userId, node);
                log.info("AI_WORKOUT_CREATED userId={} name={}", userId, createdWorkout.getName());
            }
        } catch (Exception e) {
            log.warn("AI_PARSE_FAIL — using raw reply. error={}", e.getMessage());
            displayMessage = rawReply;
        }

        // Save assistant reply
        ChatMessage aiMsg = ChatMessage.builder()
                .userId(userId)
                .sessionId(sessionId)
                .role(MessageRole.ASSISTANT)
                .content(displayMessage)
                .build();
        chatMessageRepository.save(aiMsg);

        return AiChatResponse.builder()
                .message(aiMsg)
                .taskCreated(createdTask)
                .tasksCreated(createdTasks)
                .goalCreated(createdGoal)
                .workoutCreated(createdWorkout)
                .build();
    }

    // -------------------------------------------------------------------------
    // End-of-day / On-demand Summary
    // -------------------------------------------------------------------------

    public String generateDailySummary(String userId, String userName) throws IOException {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        List<Task> allTasks = taskRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<Goal> activeGoals = goalRepository.findByUserIdAndStatus(userId, Goal.GoalStatus.ACTIVE);

        long completedToday = allTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.COMPLETED
                        && t.getCompletedAt() != null
                        && t.getCompletedAt().isAfter(startOfDay))
                .count();

        List<Task> pending = allTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.PENDING || t.getStatus() == TaskStatus.IN_PROGRESS)
                .collect(Collectors.toList());

        List<Task> overdue = pending.stream()
                .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(now))
                .collect(Collectors.toList());

        String prompt = String.format(
                "Generate a concise, encouraging end-of-day summary for %s.\n\n" +
                "DATA:\n" +
                "- Tasks completed today: %d\n" +
                "- Total pending tasks: %d\n" +
                "- Overdue tasks: %d — these are: %s\n" +
                "- Pending tasks (top 5): %s\n" +
                "- Active goals: %s\n\n" +
                "FORMAT your summary exactly like this:\n" +
                "✅ **Today's Wins** — list completed count and celebrate\n" +
                "📋 **Still Pending** — list top pending tasks\n" +
                "⚠️ **Overdue** — flag overdue items if any\n" +
                "🎯 **Your Goals** — remind them of their active goals\n" +
                "💡 **Tomorrow's Focus** — suggest the top priority for tomorrow\n" +
                "Keep it warm, motivating, and under 250 words.",
                userName != null ? userName : "you",
                completedToday,
                pending.size(),
                overdue.size(),
                overdue.stream().map(Task::getTitle).limit(3).collect(Collectors.joining(", ")),
                pending.stream().map(Task::getTitle).limit(5).collect(Collectors.joining(", ")),
                activeGoals.stream().map(Goal::getTitle).collect(Collectors.joining(", "))
        );

        String rawReply = callLLM(
                "You are a productivity coach. Respond ONLY with the formatted summary, no JSON wrapper.",
                List.of(), prompt);

        // Strip JSON if model wraps it anyway
        try {
            JsonNode node = objectMapper.readTree(rawReply.trim());
            if (node.has("message")) return node.path("message").asText();
        } catch (Exception ignored) { /* not JSON, use as-is */ }

        return rawReply.trim();
    }

    // -------------------------------------------------------------------------
    // Quick AI tip (1-2 sentences)
    // -------------------------------------------------------------------------

    public String quickSuggest(String context) throws IOException {
        if (context == null || context.isBlank()) return "";
        String prompt = "Give exactly ONE practical tip (1-2 sentences) related to: " + context + "\nNo fluff, no JSON, plain text only.";
        String raw = callLLM("You are a helpful productivity and wellness coach.", List.of(), prompt);
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(raw.trim());
            if (node.has("message")) return node.path("message").asText().trim();
        } catch (Exception ignored) { /* plain text */ }
        return raw.trim();
    }

    // -------------------------------------------------------------------------
    // Task breakdown
    // -------------------------------------------------------------------------

    public List<String> generateTaskBreakdown(String taskTitle, String taskDescription) throws IOException {
        String prompt = String.format(
                "Break down the following task into 5-8 clear, actionable sub-tasks. " +
                "Return ONLY a JSON array of strings, no extra text.\n\nTask: %s\nDescription: %s",
                taskTitle, taskDescription != null ? taskDescription : "N/A");

        String response = callLLM(
                "You are a productivity expert. Always respond with valid JSON arrays only.",
                List.of(), prompt);
        try {
            String cleaned = response.trim()
                    .replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();
            JsonNode node = objectMapper.readTree(cleaned);
            List<String> result = new ArrayList<>();
            node.forEach(n -> result.add(n.asText()));
            return result;
        } catch (Exception e) {
            log.warn("Could not parse breakdown as JSON: {}", response);
            return List.of(response.trim());
        }
    }

    // -------------------------------------------------------------------------
    // Goal analysis
    // -------------------------------------------------------------------------

    public String analyzeGoal(String goalTitle, String goalDescription, String category) throws IOException {
        String prompt = String.format(
                "Analyze this goal and give concise, actionable advice on how to achieve it. " +
                "Include potential obstacles and strategies.\n\nGoal: %s\nCategory: %s\nDescription: %s",
                goalTitle, category, goalDescription != null ? goalDescription : "N/A");

        String raw = callLLM(buildSystemPrompt(null), List.of(), prompt);
        try {
            JsonNode node = objectMapper.readTree(raw.trim()
                    .replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim());
            if (node.has("message")) return node.path("message").asText();
        } catch (Exception ignored) {}
        return raw.trim();
    }

    // -------------------------------------------------------------------------
    // Chat history helpers
    // -------------------------------------------------------------------------

    public List<ChatMessage> getChatHistory(String userId, String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            return chatMessageRepository.findByUserIdAndSessionIdOrderByTimestampAsc(userId, sessionId);
        }
        return chatMessageRepository.findByUserIdOrderByTimestampAsc(userId);
    }

    public void clearSession(String userId, String sessionId) {
        chatMessageRepository.deleteByUserIdAndSessionId(userId, sessionId);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String buildSystemPrompt(String userId) {
        LocalDate todayDate = LocalDate.now();
        String today    = todayDate.format(DateTimeFormatter.ofPattern("EEEE yyyy-MM-dd"));
        String tomorrow = todayDate.plusDays(1).format(DateTimeFormatter.ofPattern("EEEE yyyy-MM-dd"));
        String dayAfter = todayDate.plusDays(2).format(DateTimeFormatter.ofPattern("EEEE yyyy-MM-dd"));

        if (userId == null) {
            return String.format(SYSTEM_PROMPT_TEMPLATE, today, tomorrow, dayAfter, 0, "none", 0, "none", 0, "none");
        }

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        List<Task> allTasks = taskRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<Goal> goals = goalRepository.findByUserIdAndStatus(userId, Goal.GoalStatus.ACTIVE);

        List<Task> pending = allTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.PENDING || t.getStatus() == TaskStatus.IN_PROGRESS)
                .limit(8)
                .collect(Collectors.toList());

        List<Task> completedToday = allTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.COMPLETED
                        && t.getCompletedAt() != null
                        && t.getCompletedAt().isAfter(startOfDay))
                .limit(5)
                .collect(Collectors.toList());

        String pendingStr = pending.isEmpty() ? "none" :
                pending.stream().map(t -> t.getTitle() +
                        (t.getDueDate() != null ? " (due " + t.getDueDate().toLocalDate() + ")" : ""))
                        .collect(Collectors.joining("; "));

        String completedStr = completedToday.isEmpty() ? "none" :
                completedToday.stream().map(Task::getTitle).collect(Collectors.joining("; "));

        String goalsStr = goals.isEmpty() ? "none" :
                goals.stream().map(Goal::getTitle).collect(Collectors.joining("; "));

        return String.format(SYSTEM_PROMPT_TEMPLATE,
                today, tomorrow, dayAfter,
                pending.size(), pendingStr,
                completedToday.size(), completedStr,
                goals.size(), goalsStr);
    }

    private Workout createWorkoutFromAiAction(String userId, JsonNode node) {
        String name = node.path("name").asText("Workout");
        String typeStr = node.path("type").isNull() ? null : node.path("type").asText(null);
        String dateStr = node.path("workoutDate").isNull() ? null : node.path("workoutDate").asText(null);
        int duration = node.path("durationMinutes").asInt(0);
        String notes = node.path("notes").isNull() ? null : node.path("notes").asText(null);

        WorkoutType type = WorkoutType.OTHER;
        if (typeStr != null && !typeStr.isBlank() && !"null".equals(typeStr)) {
            try { type = WorkoutType.valueOf(typeStr.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }

        LocalDate workoutDate = LocalDate.now();
        if (dateStr != null && !dateStr.isBlank() && !"null".equals(dateStr)) {
            try { workoutDate = LocalDate.parse(dateStr.length() > 10 ? dateStr.substring(0, 10) : dateStr); }
            catch (DateTimeParseException e) { log.warn("Could not parse AI workout date: {}", dateStr); }
        }

        Workout workout = Workout.builder()
                .userId(userId)
                .name(name)
                .type(type)
                .workoutDate(workoutDate)
                .durationMinutes(duration)
                .notes(notes)
                .build();

        return workoutRepository.save(workout);
    }

    private Goal createGoalFromAiAction(String userId, JsonNode node) {
        String title = node.path("title").asText("Untitled Goal");
        String description = node.path("description").isNull() ? null : node.path("description").asText(null);
        String categoryStr = node.path("category").isNull() ? null : node.path("category").asText(null);
        String targetDateStr = node.path("targetDate").isNull() ? null : node.path("targetDate").asText(null);

        Goal.GoalCategory category = Goal.GoalCategory.OTHER;
        if (categoryStr != null && !categoryStr.isBlank() && !"null".equals(categoryStr)) {
            try { category = Goal.GoalCategory.valueOf(categoryStr.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }

        LocalDate targetDate = null;
        if (targetDateStr != null && !targetDateStr.isBlank() && !"null".equals(targetDateStr)) {
            try { targetDate = LocalDate.parse(targetDateStr.length() > 10 ? targetDateStr.substring(0, 10) : targetDateStr); }
            catch (DateTimeParseException e) { log.warn("Could not parse AI goal targetDate: {}", targetDateStr); }
        }

        Goal goal = Goal.builder()
                .userId(userId)
                .title(title)
                .description(description)
                .category(category)
                .targetDate(targetDate)
                .status(Goal.GoalStatus.ACTIVE)
                .build();

        return goalRepository.save(goal);
    }

    private Task createTaskFromAiAction(String userId, JsonNode node) {
        String title = node.path("title").asText("Untitled Task");
        String description = node.path("description").isNull() ? null : node.path("description").asText(null);
        String dueDateStr = node.path("dueDate").isNull() ? null : node.path("dueDate").asText(null);
        String priorityStr = node.path("priority").isNull() ? null : node.path("priority").asText(null);
        String category = node.path("category").isNull() ? null : node.path("category").asText(null);

        LocalDateTime dueDate = null;
        if (dueDateStr != null && !dueDateStr.isBlank() && !"null".equals(dueDateStr)) {
            try {
                dueDate = LocalDateTime.parse(dueDateStr);
            } catch (DateTimeParseException e) {
                try {
                    dueDate = LocalDate.parse(dueDateStr).atTime(9, 0);
                } catch (DateTimeParseException e2) {
                    log.warn("Could not parse AI dueDate: {}", dueDateStr);
                }
            }
        }

        TaskPriority priority = null;
        if (priorityStr != null && !priorityStr.isBlank() && !"null".equals(priorityStr)) {
            try { priority = TaskPriority.valueOf(priorityStr.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }

        Task task = Task.builder()
                .userId(userId)
                .title(title)
                .description(description)
                .dueDate(dueDate)
                .priority(priority)
                .category(category)
                .status(TaskStatus.PENDING)
                .build();

        return taskRepository.save(task);
    }

    private String callLLM(String systemPrompt, List<ChatMessage> history, String userMessage)
            throws IOException {

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", openAIConfig.getModel());
        body.put("max_tokens", 1024);
        body.put("temperature", 0.6);

        ArrayNode messages = body.putArray("messages");

        ObjectNode sysMsg = objectMapper.createObjectNode();
        sysMsg.put("role", "system");
        sysMsg.put("content", systemPrompt);
        messages.add(sysMsg);

        for (ChatMessage msg : history) {
            ObjectNode m = objectMapper.createObjectNode();
            m.put("role", msg.getRole() == MessageRole.USER ? "user" : "assistant");
            m.put("content", msg.getContent());
            messages.add(m);
        }

        ObjectNode userMsg = objectMapper.createObjectNode();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        RequestBody requestBody = RequestBody.create(objectMapper.writeValueAsString(body), JSON_MEDIA);
        Request httpRequest = new Request.Builder()
                .url(openAIConfig.getApiUrl())
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + openAIConfig.getApiKey())
                .addHeader("Content-Type", "application/json")
                .build();

        log.info("AI_CALL model={} historySize={}", openAIConfig.getModel(), history.size());
        long start = System.currentTimeMillis();

        try (Response response = okHttpClient.newCall(httpRequest).execute()) {
            long ms = System.currentTimeMillis() - start;
            if (!response.isSuccessful()) {
                String err = response.body() != null ? response.body().string() : "unknown";
                log.error("AI_ERROR model={} status={} durationMs={}", openAIConfig.getModel(), response.code(), ms);
                throw new IOException("AI API error " + response.code() + ": " + err);
            }
            String responseBody = response.body().string();
            JsonNode json = objectMapper.readTree(responseBody);
            String content = json.path("choices").get(0).path("message").path("content").asText();
            log.info("AI_DONE model={} durationMs={} replyLen={}", openAIConfig.getModel(), ms, content.length());
            return content;
        }
    }
}
