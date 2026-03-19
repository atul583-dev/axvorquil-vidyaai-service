package com.axvorquil.vidya.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReadinessResponse {
    private String                 sessionId;
    private String                 exam;
    private int                    readinessScore;    // 0–100
    private String                 prediction;        // "Expected score: 142–151/200"
    private String                 recommendation;    // "Ready to attempt" / "Needs 2–3 more months"
    private List<String>           weakSubjects;
    private Map<String, Integer>   subjectScores;
    private List<StudyTask>        studyPlan;         // 7-day plan
    private boolean                aiGenerated;
}
