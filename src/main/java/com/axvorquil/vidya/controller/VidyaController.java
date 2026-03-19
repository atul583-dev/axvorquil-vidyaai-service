package com.axvorquil.vidya.controller;

import com.axvorquil.vidya.dto.*;
import com.axvorquil.vidya.model.QuestionSession;
import com.axvorquil.vidya.model.ReadinessSession;
import com.axvorquil.vidya.model.Vertical;
import com.axvorquil.vidya.service.VidyaAIService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vidya")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VidyaController {

    private final VidyaAIService service;

    /** Generate practice questions for a given exam + subject + topic */
    @PostMapping("/questions/generate")
    public ApiResponse<QuestionResponse> generate(@Valid @RequestBody QuestionRequest req) {
        return ApiResponse.ok(service.generateQuestions(req));
    }

    /** Explain a wrong answer in plain language */
    @PostMapping("/questions/explain")
    public ApiResponse<ExplainResponse> explain(@Valid @RequestBody ExplainRequest req) {
        return ApiResponse.ok(service.explain(req));
    }

    /** Calculate readiness score + generate 7-day study plan */
    @PostMapping("/readiness")
    public ApiResponse<ReadinessResponse> readiness(@Valid @RequestBody ReadinessRequest req) {
        return ApiResponse.ok(service.readiness(req));
    }

    /** Question session history, optionally filtered by vertical */
    @GetMapping("/history/questions")
    public ApiResponse<List<QuestionSession>> questionHistory(
            @RequestParam(required = false) Vertical vertical) {
        return ApiResponse.ok(service.getQuestionHistory(vertical));
    }

    /** Readiness session history */
    @GetMapping("/history/readiness")
    public ApiResponse<List<ReadinessSession>> readinessHistory() {
        return ApiResponse.ok(service.getReadinessHistory());
    }
}
