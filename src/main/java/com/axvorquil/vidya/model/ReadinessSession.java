package com.axvorquil.vidya.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "vidya_readiness_sessions")
public class ReadinessSession {

    @Id
    private String id;

    private String   tenantId;
    private Vertical vertical;
    private String   exam;
    private int      daysLeft;

    /** subject → score (0-100) */
    private Map<String, Integer> subjectScores;

    /** Full readiness report JSON from Claude */
    private String   reportJson;

    private boolean  aiGenerated;

    @CreatedDate
    private Instant createdAt;
}
