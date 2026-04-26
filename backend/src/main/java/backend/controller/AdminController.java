package backend.controller;

import backend.adaptive.AdaptiveSecurityService;
import backend.adaptive.RecoveryStateMapper;
import backend.adaptive.RiskAssessment;
import backend.adaptive.ThreatSignalService;
import backend.adaptive.UserBehaviorProfileService;
import backend.audit.AuditEvent;
import backend.audit.AuditEventRepository;
import backend.audit.AuditEventType;
import backend.audit.AuditService;
import backend.dto.ApiSuccessResponse;
import backend.exception.BadRequestException;
import backend.exception.ResourceNotFoundException;
import backend.model.Message;
import backend.model.MessageStatus;
import backend.model.Puzzle;
import backend.model.User;
import backend.model.UserBehaviorProfile;
import backend.repository.MessageRepository;
import backend.repository.PuzzleRepository;
import backend.repository.UserBehaviorProfileRepository;
import backend.service.UserService;
import backend.util.ApiResponseUtil;
import backend.util.RequestContextUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@Validated
public class AdminController {

    private final UserService userService;
    private final AdaptiveSecurityService adaptiveSecurityService;
    private final AuditService auditService;
    private final RequestContextUtil requestContextUtil;
    private final ThreatSignalService threatSignalService;
    private final AuditEventRepository auditEventRepository;
    private final MessageRepository messageRepository;
    private final PuzzleRepository puzzleRepository;
    private final UserBehaviorProfileRepository behaviorRepository;
    private final UserBehaviorProfileService behaviorService;

    public AdminController(
            UserService userService,
            AdaptiveSecurityService adaptiveSecurityService,
            AuditService auditService,
            RequestContextUtil requestContextUtil,
            ThreatSignalService threatSignalService,
            AuditEventRepository auditEventRepository,
            MessageRepository messageRepository,
            PuzzleRepository puzzleRepository,
            UserBehaviorProfileRepository behaviorRepository,
            UserBehaviorProfileService behaviorService
    ) {
        this.userService = userService;
        this.adaptiveSecurityService = adaptiveSecurityService;
        this.auditService = auditService;
        this.requestContextUtil = requestContextUtil;
        this.threatSignalService = threatSignalService;
        this.auditEventRepository = auditEventRepository;
        this.messageRepository = messageRepository;
        this.puzzleRepository = puzzleRepository;
        this.behaviorRepository = behaviorRepository;
        this.behaviorService = behaviorService;
    }

    @PostMapping("/lock-user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> lockUser(
            @RequestParam("username") @NotBlank String username,
            @RequestParam(value = "minutes", defaultValue = "15") int minutes,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        if (minutes <= 0 || minutes > 24 * 60) {
            throw new BadRequestException("minutes must be in (0, 1440]");
        }

        User user = userService.getRequiredByUsername(username.trim());
        userService.lockAccount(user, java.time.LocalDateTime.now().plusMinutes(minutes));

        String ip = requestContextUtil.clientIp(httpRequest);
        String ua = requestContextUtil.userAgent(httpRequest);
        auditService.record(
                AuditEventType.ADMIN_ACTION,
                authentication.getName(),
                user.getUsername(),
                ip,
                ua,
                null,
                Map.of("action", "lock_user", "minutes", minutes)
        );

        return ResponseEntity.ok(ApiResponseUtil.success("User locked", httpRequest.getRequestURI(), Map.of("username", user.getUsername(), "locked", true)));
    }

    @PostMapping("/unlock-user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> unlockUser(
            @RequestParam("username") @NotBlank String username,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        User user = userService.getRequiredByUsername(username.trim());
        userService.unlockAccount(user);

        String ip = requestContextUtil.clientIp(httpRequest);
        String ua = requestContextUtil.userAgent(httpRequest);
        auditService.record(
                AuditEventType.ADMIN_ACTION,
                authentication.getName(),
                user.getUsername(),
                ip,
                ua,
                null,
                Map.of("action", "unlock_user")
        );

        return ResponseEntity.ok(ApiResponseUtil.success("User unlocked", httpRequest.getRequestURI(), Map.of("username", user.getUsername(), "locked", false)));
    }

    @PostMapping("/risk-score")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> riskScore(
            @RequestParam("username") @NotBlank String username,
            HttpServletRequest httpRequest
    ) {
        User user = userService.getRequiredByUsername(username.trim());
        String ip = requestContextUtil.clientIp(httpRequest);
        String ua = requestContextUtil.userAgent(httpRequest);
        RiskAssessment assessment = adaptiveSecurityService.assess(user, ip, ua, 0);

        Map<String, Object> data = Map.of(
                "username", user.getUsername(),
                "riskScore", assessment.getRiskScore(),
                "riskLevel", assessment.getRiskLevel().name(),
                "signals", assessment.getSignals(),
                "accountLocked", user.isAccountLocked(),
                "lockedUntil", user.getLockedUntil()
        );

        return ResponseEntity.ok(ApiResponseUtil.success("Risk score computed", httpRequest.getRequestURI(), data));
    }

    @GetMapping("/audit/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<List<AuditEvent>>> recentAudit(HttpServletRequest httpRequest) {
        List<AuditEvent> events = auditEventRepository.findTop200ByOrderByCreatedAtDesc();
        return ResponseEntity.ok(ApiResponseUtil.success("Recent audit events fetched", httpRequest.getRequestURI(), events));
    }

    @GetMapping("/threat-level")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> threatLevel(HttpServletRequest httpRequest) {
        double v = threatSignalService.currentAttackIntensity01();
        return ResponseEntity.ok(ApiResponseUtil.success("Threat level fetched", httpRequest.getRequestURI(), Map.of("attackIntensity01", v)));
    }

    @PostMapping("/threat-level")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> setThreatLevel(
            @RequestParam("attackIntensity01") double attackIntensity01,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        threatSignalService.setAttackIntensity01(attackIntensity01);
        String ip = requestContextUtil.clientIp(httpRequest);
        String ua = requestContextUtil.userAgent(httpRequest);
        auditService.record(
                AuditEventType.ADMIN_ACTION,
                authentication.getName(),
                null,
                ip,
                ua,
                null,
                Map.of("action", "set_threat_level", "attackIntensity01", threatSignalService.currentAttackIntensity01())
        );
        return ResponseEntity.ok(ApiResponseUtil.success("Threat level updated", httpRequest.getRequestURI(), Map.of("attackIntensity01", threatSignalService.currentAttackIntensity01())));
    }

    @PostMapping("/hold-message")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> holdMessage(
            @RequestParam("messageId") long messageId,
            @RequestParam(value = "reason", defaultValue = "ADMIN_REVIEW_REQUIRED") String reason,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        if (messageId <= 0) {
            throw new BadRequestException("messageId must be positive");
        }
        Message msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + messageId));
        msg.setStatus(MessageStatus.HELD);
        msg.setHoldReason(reason == null ? "ADMIN_REVIEW_REQUIRED" : reason.trim());
        messageRepository.save(msg);

        String ip = requestContextUtil.clientIp(httpRequest);
        String ua = requestContextUtil.userAgent(httpRequest);
        auditService.record(AuditEventType.ADMIN_ACTION, authentication.getName(), msg.getReceiver().getUsername(), ip, ua, null,
                Map.of("action", "hold_message", "messageId", messageId, "reason", msg.getHoldReason()));

        return ResponseEntity.ok(ApiResponseUtil.success("Message held", httpRequest.getRequestURI(), Map.of("messageId", messageId, "status", msg.getStatus().name())));
    }

    @PostMapping("/release-message")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> releaseMessage(
            @RequestParam("messageId") long messageId,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        if (messageId <= 0) {
            throw new BadRequestException("messageId must be positive");
        }
        Message msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + messageId));
        if (msg.getStatus() != MessageStatus.HELD) {
            throw new BadRequestException("Message is not currently held");
        }
        msg.setStatus(MessageStatus.LOCKED);
        msg.setHoldReason(null);
        messageRepository.save(msg);

        // Record a recovery event in the receiver's behavior profile so the SOC and the
        // adaptive engine see that the user has been through admin-supervised recovery.
        if (msg.getReceiver() != null) {
            behaviorService.recordRecoveryEvent(msg.getReceiver());
        }

        String ip = requestContextUtil.clientIp(httpRequest);
        String ua = requestContextUtil.userAgent(httpRequest);
        auditService.record(AuditEventType.ADMIN_ACTION, authentication.getName(), msg.getReceiver().getUsername(), ip, ua, null,
                Map.of("action", "release_message", "messageId", messageId));

        return ResponseEntity.ok(ApiResponseUtil.success("Message released", httpRequest.getRequestURI(), Map.of("messageId", messageId, "status", msg.getStatus().name())));
    }

    /**
     * Lists messages currently HELD by the system. Admins see metadata only, never plaintext.
     */
    @GetMapping("/held-messages")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<List<Map<String, Object>>>> heldMessages(HttpServletRequest httpRequest) {
        List<Message> held = messageRepository.findAll().stream()
                .filter(m -> m.getStatus() == MessageStatus.HELD)
                .toList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Message m : held) {
            Puzzle puzzle = puzzleRepository.findByMessageId(m.getId()).orElse(null);
            Map<String, Object> entry = new HashMap<>();
            entry.put("messageId", m.getId());
            entry.put("senderUsername", m.getSender() == null ? null : m.getSender().getUsername());
            entry.put("receiverUsername", m.getReceiver() == null ? null : m.getReceiver().getUsername());
            entry.put("requestedMode", m.getRequestedAlgorithmType() == null ? null : m.getRequestedAlgorithmType().name());
            entry.put("enforcedMode", m.getAlgorithmType() == null ? null : m.getAlgorithmType().name());
            entry.put("riskScore", m.getRiskScoreAtSend());
            entry.put("riskLevel", m.getRiskLevelAtSend());
            entry.put("holdReason", m.getHoldReason());
            entry.put("recoveryState", RecoveryStateMapper.resolve(m, puzzle).name());
            entry.put("createdAt", m.getCreatedAt());
            result.add(entry);
        }
        return ResponseEntity.ok(ApiResponseUtil.success("Held messages fetched", httpRequest.getRequestURI(), result));
    }

    /**
     * Lists users currently flagged by the behavior profile engine (consecutive failures
     * &gt; 0 or many puzzle failures). Used by the admin SOC to see who needs supervision.
     */
    @GetMapping("/users-at-risk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<List<Map<String, Object>>>> usersAtRisk(HttpServletRequest httpRequest) {
        List<UserBehaviorProfile> profiles = behaviorRepository.findTop50ByOrderByConsecutiveFailuresDescLastFailureAtDesc();
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserBehaviorProfile p : profiles) {
            User user = userService.getById(p.getUserId());
            Map<String, Object> entry = new HashMap<>();
            entry.put("username", user == null ? null : user.getUsername());
            entry.put("puzzleAttempts", p.getPuzzleAttempts());
            entry.put("puzzleSuccesses", p.getPuzzleSuccesses());
            entry.put("puzzleFailures", p.getPuzzleFailures());
            entry.put("consecutiveFailures", p.getConsecutiveFailures());
            entry.put("avgSolveTimeMs", p.getAvgSolveTimeMs());
            entry.put("recoveryEvents", p.getRecoveryEvents());
            entry.put("lastFailureAt", p.getLastFailureAt());
            entry.put("lastSuccessAt", p.getLastSuccessAt());
            result.add(entry);
        }
        return ResponseEntity.ok(ApiResponseUtil.success("Users at risk fetched", httpRequest.getRequestURI(), result));
    }

    /**
     * Resets a user's failed challenge counters after admin review. The audit trail and
     * the recovery_events counter make sure this is observable.
     */
    @PostMapping("/reset-failures")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> resetFailures(
            @RequestParam("username") @NotBlank String username,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        User user = userService.getRequiredByUsername(username.trim());
        UserBehaviorProfile updated = behaviorService.resetCounters(user);

        String ip = requestContextUtil.clientIp(httpRequest);
        String ua = requestContextUtil.userAgent(httpRequest);
        auditService.record(
                AuditEventType.ADMIN_ACTION,
                authentication.getName(),
                user.getUsername(),
                ip,
                ua,
                null,
                Map.of("action", "reset_failures", "consecutiveFailures", updated.getConsecutiveFailures())
        );
        return ResponseEntity.ok(ApiResponseUtil.success(
                "Failure counters reset",
                httpRequest.getRequestURI(),
                Map.of("username", user.getUsername(), "consecutiveFailures", updated.getConsecutiveFailures())
        ));
    }
}

