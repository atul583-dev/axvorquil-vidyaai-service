package com.axvorquil.vidya.dto;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StudyTask {
    private String       day;    // "Day 1", "Day 2", …
    private List<String> tasks;  // ["45 min Profit & Loss", "15 min Current Affairs"]
}
