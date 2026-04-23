package backend.audit;

import backend.util.HashUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final HashUtil hashUtil;
    private final ObjectMapper objectMapper;

    public AuditService(AuditEventRepository auditEventRepository, HashUtil hashUtil, ObjectMapper objectMapper) {
        this.auditEventRepository = auditEventRepository;
        this.hashUtil = hashUtil;
        this.objectMapper = objectMapper;
    }

    public void record(AuditEventType type, String actorUsername, String subjectUsername, String ip, String fingerprint, Double riskScore, Map<String, Object> details) {
        AuditEvent event = new AuditEvent();
        event.setEventType(type);
        event.setActorUsername(actorUsername);
        event.setSubjectUsername(subjectUsername);
        event.setIpHash(ip == null ? null : hashUtil.sha256Hex(ip));
        event.setFingerprintHash(fingerprint == null ? null : hashUtil.sha256Hex(fingerprint));
        event.setRiskScore(riskScore);
        event.setCreatedAt(LocalDateTime.now());
        event.setDetails(toJson(details));
        auditEventRepository.save(event);
    }

    private String toJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{\"error\":\"failed_to_serialize\"}";
        }
    }
}

