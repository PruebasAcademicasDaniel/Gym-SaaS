package com.gymflow.audit.application;

import com.gymflow.audit.domain.AuditAction;
import com.gymflow.audit.domain.AuditLog;
import com.gymflow.audit.infrastructure.persistence.AuditLogRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(UUID gymId, UUID actorUserId, AuditAction action, String detail) {
        auditLogRepository.save(new AuditLog(gymId, actorUserId, action, detail));
    }
}
