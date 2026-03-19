package com.axvorquil.vidya.service;

import com.axvorquil.vidya.context.TenantContext;
import com.axvorquil.vidya.dto.*;
import com.axvorquil.vidya.model.QuestionSession;
import com.axvorquil.vidya.model.ReadinessSession;
import com.axvorquil.vidya.model.Vertical;
import com.axvorquil.vidya.repository.QuestionSessionRepository;
import com.axvorquil.vidya.repository.ReadinessSessionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VidyaAIService {

    private final QuestionSessionRepository  questionRepo;
    private final ReadinessSessionRepository readinessRepo;
    private final RestTemplate   restTemplate;
    private final ObjectMapper   objectMapper;

    @Value("${anthropic.api-key:}")
    private String apiKey;
    @Value("${anthropic.model:claude-3-5-haiku-20241022}")
    private String model;
    @Value("${anthropic.max-tokens:2048}")
    private int maxTokens;

    // ─────────────────────────────────────────────────────────────
    // QUESTION GENERATION
    // ─────────────────────────────────────────────────────────────

    public QuestionResponse generateQuestions(QuestionRequest req) {
        String tenantId = TenantContext.get();
        String difficulty = req.getDifficulty() != null ? req.getDifficulty() : "MEDIUM";
        String topic      = req.getTopic()      != null ? req.getTopic()      : req.getSubject();

        String prompt = buildQuestionPrompt(req, topic, difficulty);

        boolean aiGenerated = false;
        List<GeneratedQuestion> questions;
        String rawJson;

        try {
            rawJson = callClaude(prompt);
            questions = parseQuestions(rawJson);
            aiGenerated = true;
        } catch (Exception e) {
            log.warn("Claude question generation failed: {}", e.getMessage());
            questions = templateQuestions(req, topic, difficulty);
            rawJson   = "template";
        }

        QuestionSession saved = questionRepo.save(QuestionSession.builder()
            .tenantId(tenantId)
            .vertical(req.getVertical())
            .exam(req.getExam())
            .subject(req.getSubject())
            .topic(topic)
            .difficulty(difficulty)
            .language(req.getLanguage())
            .rawQuestions(rawJson)
            .count(questions.size())
            .aiGenerated(aiGenerated)
            .build());

        return QuestionResponse.builder()
            .sessionId(saved.getId())
            .exam(req.getExam())
            .subject(req.getSubject())
            .topic(topic)
            .questions(questions)
            .aiGenerated(aiGenerated)
            .build();
    }

    // ─────────────────────────────────────────────────────────────
    // ANSWER EXPLANATION
    // ─────────────────────────────────────────────────────────────

    public ExplainResponse explain(ExplainRequest req) {
        String prompt = """
            A student preparing for %s answered a question incorrectly.

            Question: %s

            Student's answer: %s
            Correct answer: %s

            Please explain in %s:
            1. Why the correct answer is right — with clear calculation or reasoning steps
            2. What concept they should review
            3. One quick trick to solve similar questions faster

            Keep the tone encouraging and concise (max 200 words).
            """.formatted(req.getExam(), req.getQuestion(),
                         req.getUserAnswer(), req.getCorrectAnswer(),
                         req.getLanguage());

        boolean aiGenerated = false;
        String explanation;
        try {
            explanation = callClaude(prompt);
            aiGenerated = true;
        } catch (Exception e) {
            log.warn("Claude explanation failed: {}", e.getMessage());
            explanation = templateExplanation(req);
        }

        return ExplainResponse.builder()
            .explanation(explanation)
            .aiGenerated(aiGenerated)
            .build();
    }

    // ─────────────────────────────────────────────────────────────
    // READINESS SCORE + STUDY PLAN
    // ─────────────────────────────────────────────────────────────

    public ReadinessResponse readiness(ReadinessRequest req) {
        String tenantId = TenantContext.get();

        String scoresText = buildScoresText(req.getSubjectScores());

        String prompt = """
            A student is preparing for %s with %d days remaining.

            Subject-wise mock test scores (out of 100):
            %s

            Based on typical %s cutoffs and exam patterns, respond ONLY with a valid JSON object (no markdown, no extra text):
            {
              "readinessScore": <integer 0-100>,
              "prediction": "<expected score range, e.g. 142-151/200>",
              "recommendation": "<clear actionable recommendation>",
              "weakSubjects": ["<subject1>", "<subject2>"],
              "studyPlan": [
                { "day": "Day 1", "tasks": ["<task1>", "<task2>"] },
                { "day": "Day 2", "tasks": ["<task1>", "<task2>"] },
                { "day": "Day 3", "tasks": ["<task1>", "<task2>"] },
                { "day": "Day 4", "tasks": ["<task1>", "<task2>"] },
                { "day": "Day 5", "tasks": ["<task1>", "<task2>"] },
                { "day": "Day 6", "tasks": ["<task1>", "<task2>"] },
                { "day": "Day 7", "tasks": ["<task1>", "<task2>", "Full mock test"] }
              ]
            }
            """.formatted(req.getExam(), req.getDaysLeft(), scoresText, req.getExam());

        boolean aiGenerated = false;
        ReadinessResponse response;

        try {
            String raw = callClaude(prompt);
            response = parseReadiness(req, raw);
            aiGenerated = true;

            readinessRepo.save(ReadinessSession.builder()
                .tenantId(tenantId)
                .vertical(req.getVertical())
                .exam(req.getExam())
                .daysLeft(req.getDaysLeft())
                .subjectScores(req.getSubjectScores())
                .reportJson(raw)
                .aiGenerated(true)
                .build());
        } catch (Exception e) {
            log.warn("Claude readiness failed: {}", e.getMessage());
            response = templateReadiness(req);

            readinessRepo.save(ReadinessSession.builder()
                .tenantId(tenantId)
                .vertical(req.getVertical())
                .exam(req.getExam())
                .daysLeft(req.getDaysLeft())
                .subjectScores(req.getSubjectScores())
                .reportJson("template")
                .aiGenerated(false)
                .build());
        }

        response.setAiGenerated(aiGenerated);
        return response;
    }

    // ─────────────────────────────────────────────────────────────
    // HISTORY
    // ─────────────────────────────────────────────────────────────

    public List<QuestionSession> getQuestionHistory(Vertical vertical) {
        String tid = TenantContext.get();
        return vertical != null
            ? questionRepo.findByTenantIdAndVerticalOrderByCreatedAtDesc(tid, vertical)
            : questionRepo.findByTenantIdOrderByCreatedAtDesc(tid);
    }

    public List<ReadinessSession> getReadinessHistory() {
        return readinessRepo.findByTenantIdOrderByCreatedAtDesc(TenantContext.get());
    }

    // ─────────────────────────────────────────────────────────────
    // Claude API
    // ─────────────────────────────────────────────────────────────

    private String callClaude(String prompt) {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("No API key");

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("x-api-key", apiKey);
        h.set("anthropic-version", "2023-06-01");

        Map<String, Object> body = Map.of(
            "model", model,
            "max_tokens", maxTokens,
            "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        ResponseEntity<String> resp = restTemplate.exchange(
            "https://api.anthropic.com/v1/messages",
            HttpMethod.POST,
            new HttpEntity<>(body, h),
            String.class
        );

        JsonNode root = parseJson(resp.getBody());
        return root.path("content").get(0).path("text").asText();
    }

    // ─────────────────────────────────────────────────────────────
    // Prompt builders
    // ─────────────────────────────────────────────────────────────

    private String buildQuestionPrompt(QuestionRequest req, String topic, String difficulty) {
        return """
            Generate %d multiple choice questions for the %s exam on the topic "%s" (subject: %s).
            Difficulty: %s. Language: %s.

            Follow the official %s question pattern exactly.
            Each question must have exactly 4 options (A, B, C, D).

            Return ONLY a valid JSON array — no markdown, no extra text:
            [
              {
                "question": "<question text>",
                "options": ["A) <option>", "B) <option>", "C) <option>", "D) <option>"],
                "correctOption": "<A|B|C|D>",
                "explanation": "<brief explanation of why the answer is correct>",
                "topic": "%s",
                "difficulty": "%s"
              }
            ]
            """.formatted(req.getCount(), req.getExam(), topic, req.getSubject(),
                         difficulty, req.getLanguage(), req.getExam(), topic, difficulty);
    }

    // ─────────────────────────────────────────────────────────────
    // Parsers
    // ─────────────────────────────────────────────────────────────

    private List<GeneratedQuestion> parseQuestions(String raw) {
        try {
            // Claude sometimes wraps JSON in markdown; strip it
            String json = raw.trim();
            if (json.startsWith("```")) {
                json = json.replaceAll("```[a-z]*\\n?", "").replace("```", "").trim();
            }
            return objectMapper.readValue(json, new TypeReference<List<GeneratedQuestion>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse question JSON: " + e.getMessage());
        }
    }

    private ReadinessResponse parseReadiness(ReadinessRequest req, String raw) {
        try {
            String json = raw.trim();
            if (json.startsWith("```")) {
                json = json.replaceAll("```[a-z]*\\n?", "").replace("```", "").trim();
            }
            JsonNode node = parseJson(json);

            List<String> weakSubjects = new ArrayList<>();
            node.path("weakSubjects").forEach(n -> weakSubjects.add(n.asText()));

            List<StudyTask> plan = new ArrayList<>();
            node.path("studyPlan").forEach(day -> {
                List<String> tasks = new ArrayList<>();
                day.path("tasks").forEach(t -> tasks.add(t.asText()));
                plan.add(new StudyTask(day.path("day").asText(), tasks));
            });

            return ReadinessResponse.builder()
                .exam(req.getExam())
                .readinessScore(node.path("readinessScore").asInt(60))
                .prediction(node.path("prediction").asText())
                .recommendation(node.path("recommendation").asText())
                .weakSubjects(weakSubjects)
                .subjectScores(req.getSubjectScores())
                .studyPlan(plan)
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse readiness JSON: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Template fallbacks
    // ─────────────────────────────────────────────────────────────

    private List<GeneratedQuestion> templateQuestions(QuestionRequest req, String topic, String difficulty) {
        String exam = req.getExam();
        List<GeneratedQuestion> bank = new ArrayList<>(List.of(
            GeneratedQuestion.builder()
                .question("If a train travels 120 km in 2 hours, what is its speed?")
                .options(List.of("A) 50 km/h", "B) 60 km/h", "C) 70 km/h", "D) 80 km/h"))
                .correctOption("B").topic(topic).difficulty(difficulty)
                .explanation("Speed = Distance ÷ Time = 120 ÷ 2 = 60 km/h.").build(),
            GeneratedQuestion.builder()
                .question("What percentage of 200 is 50?")
                .options(List.of("A) 15%", "B) 20%", "C) 25%", "D) 30%"))
                .correctOption("C").topic(topic).difficulty(difficulty)
                .explanation("(50 ÷ 200) × 100 = 25%.").build(),
            GeneratedQuestion.builder()
                .question("A shopkeeper marks an item 40% above cost price and gives 20% discount. What is the profit percentage?")
                .options(List.of("A) 8%", "B) 10%", "C) 12%", "D) 20%"))
                .correctOption("C").topic(topic).difficulty(difficulty)
                .explanation("SP = 140% × 80% = 112% of CP. Profit = 12%.").build(),
            GeneratedQuestion.builder()
                .question("Which article of the Indian Constitution deals with Right to Equality?")
                .options(List.of("A) Article 12", "B) Article 14", "C) Article 19", "D) Article 21"))
                .correctOption("B").topic(topic).difficulty(difficulty)
                .explanation("Article 14 guarantees equality before law and equal protection of laws.").build(),
            GeneratedQuestion.builder()
                .question("The Headquarters of International Monetary Fund (IMF) is located in?")
                .options(List.of("A) New York", "B) London", "C) Washington D.C.", "D) Geneva"))
                .correctOption("C").topic(topic).difficulty(difficulty)
                .explanation("IMF headquarters is in Washington D.C., USA.").build(),
            GeneratedQuestion.builder()
                .question("A and B can complete a work in 12 days and 18 days respectively. In how many days will they finish it together?")
                .options(List.of("A) 6 days", "B) 7.2 days", "C) 8 days", "D) 9 days"))
                .correctOption("B").topic(topic).difficulty(difficulty)
                .explanation("Combined rate = 1/12 + 1/18 = 5/36. Days = 36/5 = 7.2 days.").build(),
            GeneratedQuestion.builder()
                .question("Which of the following is NOT a greenhouse gas?")
                .options(List.of("A) CO2", "B) Methane", "C) Nitrogen", "D) Water Vapour"))
                .correctOption("C").topic(topic).difficulty(difficulty)
                .explanation("Nitrogen (N2) is not a greenhouse gas. It does not absorb infrared radiation.").build(),
            GeneratedQuestion.builder()
                .question("Find the odd one out: 2, 5, 10, 17, 26, 37, 50, 64")
                .options(List.of("A) 37", "B) 50", "C) 64", "D) 26"))
                .correctOption("C").topic(topic).difficulty(difficulty)
                .explanation("Series: n²+1 = 5,10,17,26,37,50,65. 64 breaks the pattern (should be 65).").build(),
            GeneratedQuestion.builder()
                .question("Simple interest on Rs. 4000 at 5% per annum for 3 years is?")
                .options(List.of("A) Rs. 400", "B) Rs. 500", "C) Rs. 600", "D) Rs. 700"))
                .correctOption("C").topic(topic).difficulty(difficulty)
                .explanation("SI = P×R×T/100 = 4000×5×3/100 = Rs. 600.").build(),
            GeneratedQuestion.builder()
                .question("Who was the first woman President of India?")
                .options(List.of("A) Indira Gandhi", "B) Sonia Gandhi", "C) Pratibha Patil", "D) Sarojini Naidu"))
                .correctOption("C").topic(topic).difficulty(difficulty)
                .explanation("Pratibha Patil served as the 12th President of India (2007–2012).").build()
        ));

        Collections.shuffle(bank);
        int count = Math.min(req.getCount(), bank.size());
        return bank.subList(0, count);
    }

    private String templateExplanation(ExplainRequest req) {
        return "The correct answer for this " + req.getExam() + " question is: " + req.getCorrectAnswer() + ".\n\n" +
            "Review the underlying concept carefully and practice similar questions. " +
            "For this type of question, focus on identifying the key formula or rule first, " +
            "then apply it step by step. Don't rush — read each option before selecting.\n\n" +
            "Tip: Practise at least 10 similar questions daily to build speed and accuracy.";
    }

    private ReadinessResponse templateReadiness(ReadinessRequest req) {
        int avg = req.getSubjectScores().values().stream().mapToInt(v -> v).sum()
                / Math.max(1, req.getSubjectScores().size());

        List<String> weak = req.getSubjectScores().entrySet().stream()
            .filter(e -> e.getValue() < 65)
            .map(Map.Entry::getKey)
            .limit(2)
            .toList();

        return ReadinessResponse.builder()
            .exam(req.getExam())
            .readinessScore(avg)
            .prediction("Based on your scores, estimated range: " + (avg * 2) + "–" + (avg * 2 + 10) + "/200")
            .recommendation(avg >= 70
                ? "You are approaching readiness. Focus on weak areas and take 2–3 full mocks."
                : "You need more preparation. Target weak subjects daily for at least 4–6 more weeks.")
            .weakSubjects(weak)
            .subjectScores(req.getSubjectScores())
            .studyPlan(List.of(
                new StudyTask("Day 1", List.of("60 min weak subject revision", "20 min current affairs")),
                new StudyTask("Day 2", List.of("45 min Quantitative Aptitude", "30 min Reasoning")),
                new StudyTask("Day 3", List.of("60 min General Awareness", "20 min English")),
                new StudyTask("Day 4", List.of("45 min weak subject deep dive", "30 min PYQ practice")),
                new StudyTask("Day 5", List.of("60 min full subject revision", "20 min current affairs")),
                new StudyTask("Day 6", List.of("Timed sectional test", "Review mistakes")),
                new StudyTask("Day 7", List.of("Full mock test (exam pattern)", "Analyse time per section", "Review all wrong answers"))
            ))
            .build();
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────

    private String buildScoresText(Map<String, Integer> scores) {
        StringBuilder sb = new StringBuilder();
        scores.forEach((subject, score) ->
            sb.append("- ").append(subject).append(": ").append(score).append("/100\n"));
        return sb.toString();
    }

    private JsonNode parseJson(String json) {
        try { return objectMapper.readTree(json); }
        catch (Exception e) { throw new RuntimeException("JSON parse error: " + e.getMessage()); }
    }
}
