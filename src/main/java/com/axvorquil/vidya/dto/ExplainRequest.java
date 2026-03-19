package com.axvorquil.vidya.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExplainRequest {

    @NotBlank
    private String exam;

    @NotBlank
    private String question;

    @NotBlank
    private String userAnswer;

    @NotBlank
    private String correctAnswer;

    private String language = "English";
}
