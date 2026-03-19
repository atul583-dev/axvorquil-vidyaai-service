package com.axvorquil.vidya.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ExplainResponse {
    private String  explanation;
    private boolean aiGenerated;
}
