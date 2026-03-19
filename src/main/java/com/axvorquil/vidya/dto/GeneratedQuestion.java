package com.axvorquil.vidya.dto;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GeneratedQuestion {
    private String       question;
    private List<String> options;       // ["A) ...", "B) ...", ...]
    private String       correctOption; // "A", "B", "C", or "D"
    private String       explanation;
    private String       topic;
    private String       difficulty;
}
