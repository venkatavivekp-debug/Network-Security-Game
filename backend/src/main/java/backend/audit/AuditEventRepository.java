package backend.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findTop100BySubjectUsernameOrderByCreatedAtDesc(String subjectUsername);
    List<AuditEvent> findTop200ByOrderByCreatedAtDesc();

    @Query("select count(e) from AuditEvent e where e.eventType = :type and e.createdAt >= :since")
    long countByTypeSince(@Param("type") AuditEventType type, @Param("since") LocalDateTime since);
}

