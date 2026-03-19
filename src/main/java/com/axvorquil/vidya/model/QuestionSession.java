package com.axvorquil.vidya.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "vidya_question_sessions")
public class QuestionSession {

    @Id
    private String id;

    private String   tenantId;
    private Vertical vertical;
    private String   exam;
    private String   subject;
    private String   topic;
    private String   difficulty;
    private String   language;

    /** Raw JSON string returned by Claude (or template) */
    private String   rawQuestions;

    /** Number of questions requested */
    private int      count;

    private boolean  aiGenerated;

    @CreatedDate
    private Instant createdAt;
}
