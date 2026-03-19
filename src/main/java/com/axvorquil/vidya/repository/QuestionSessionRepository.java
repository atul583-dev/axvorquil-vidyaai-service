package com.axvorquil.vidya.repository;

import com.axvorquil.vidya.model.QuestionSession;
import com.axvorquil.vidya.model.Vertical;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface QuestionSessionRepository extends MongoRepository<QuestionSession, String> {
    List<QuestionSession> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    List<QuestionSession> findByTenantIdAndVerticalOrderByCreatedAtDesc(String tenantId, Vertical vertical);
}
