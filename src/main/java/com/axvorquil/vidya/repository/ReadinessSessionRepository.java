package com.axvorquil.vidya.repository;

import com.axvorquil.vidya.model.ReadinessSession;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReadinessSessionRepository extends MongoRepository<ReadinessSession, String> {
    List<ReadinessSession> findByTenantIdOrderByCreatedAtDesc(String tenantId);
}
