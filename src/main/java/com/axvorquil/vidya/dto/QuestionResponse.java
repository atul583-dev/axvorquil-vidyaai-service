package com.axvorquil.vidya.dto;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class QuestionResponse {
    private String                 sessionId;
    private String                 exam;
    private String                 subject;
    private String                 topic;
    private List<GeneratedQuestion> questions;
    private boolean                aiGenerated;
}
