package com.axvorquil.vidya.dto;

import com.axvorquil.vidya.model.Vertical;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuestionRequest {

    @NotNull
    private Vertical vertical;

    @NotBlank
    private String exam;       // e.g. "SSC CGL", "IBPS PO", "MPSC"

    @NotBlank
    private String subject;    // e.g. "Quantitative Aptitude"

    private String topic;      // optional sub-topic, e.g. "Percentages"

    private String difficulty; // EASY | MEDIUM | HARD  (default MEDIUM)

    @Min(1) @Max(10)
    private int count = 5;

    private String language = "English";  // English | Hindi
}
