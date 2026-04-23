package backend.service;

import backend.crypto.CPHSDecryptionResult;
import backend.crypto.EncryptionPackage;
import backend.dto.MessageDecryptResponse;
import backend.dto.MessageSendRequest;
import backend.dto.MessageSendResponse;
import backend.dto.MessageSummaryResponse;
import backend.adaptive.AdaptiveDecision;
import backend.adaptive.AdaptiveModePolicyService;
import backend.adaptive.PuzzleDifficulty;
import backend.audit.AuditEventType;
import backend.audit.AuditService;
import backend.exception.BadRequestException;
import backend.exception.ResourceNotFoundException;
import backend.model.AlgorithmType;
import backend.model.Message;
import backend.model.MessageStatus;
import backend.model.Role;
import backend.model.User;
import backend.repository.MessageRepository;
import backend.repository.PuzzleRepository;
import backend.util.HashUtil;
import backend.util.RequestContextUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageService.class);

    private final MessageRepository messageRepository;
    private final UserService userService;
    private final CryptoService cryptoService;
    private final SHCSService shcsService;
    private final CPHSService cphsService;
    private final MessagePuzzleService messagePuzzleService;
    private final PuzzleRepository puzzleRepository;
    private final AdaptiveModePolicyService adaptiveModePolicyService;
    private final AuditService auditService;
    private final RequestContextUtil requestContextUtil;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final HashUtil hashUtil;

    public MessageService(
            MessageRepository messageRepository,
            UserService userService,
            CryptoService cryptoService,
            SHCSService shcsService,
            CPHSService cphsService,
            MessagePuzzleService messagePuzzleService,
            PuzzleRepository puzzleRepository,
            AdaptiveModePolicyService adaptiveModePolicyService,
            AuditService auditService,
            RequestContextUtil requestContextUtil,
            ObjectMapper objectMapper,
            Validator validator,
            HashUtil hashUtil
    ) {
        this.messageRepository = messageRepository;
        this.userService = userService;
        this.cryptoService = cryptoService;
        this.shcsService = shcsService;
        this.cphsService = cphsService;
        this.messagePuzzleService = messagePuzzleService;
        this.puzzleRepository = puzzleRepository;
        this.adaptiveModePolicyService = adaptiveModePolicyService;
        this.auditService = auditService;
        this.requestContextUtil = requestContextUtil;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.hashUtil = hashUtil;
    }

    @Transactional
    public MessageSendResponse sendMessage(String senderUsername, MessageSendRequest request) {
        return sendMessage(senderUsername, request, null);
    }

    @Transactional
    public MessageSendResponse sendMessage(String senderUsername, MessageSendRequest request, HttpServletRequest httpServletRequest) {
        validateBean(request);

        User sender = userService.getRequiredByUsername(senderUsername);
        User receiver = userService.getRequiredByUsername(request.getReceiverUsername().trim());

        if (sender.getRole() != Role.SENDER) {
            throw new BadRequestException("Only users with SENDER role can send messages");
        }
        if (receiver.getRole() != Role.RECEIVER) {
            throw new BadRequestException("Messages can be sent only to users with RECEIVER role");
        }

        String ip = requestContextUtil.clientIp(httpServletRequest);
        String ua = requestContextUtil.userAgent(httpServletRequest);
        AdaptiveDecision decision = adaptiveModePolicyService.decide(sender, request.getAlgorithmType(), ip, ua, 0, 0);

        EncryptionPackage packageData = encryptByAlgorithm(
                decision.getEffectiveMode(),
                request.getContent(),
                sender.getUsername(),
                receiver.getUsername(),
                decision.getDifficulty()
        );

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setEncryptedContent(packageData.getEncryptedContent());
        message.setOriginalHash(hashPlaintext(request.getContent()));
        message.setRequestedAlgorithmType(decision.getRequestedMode());
        message.setAlgorithmType(decision.getEffectiveMode());
        message.setRiskScoreAtSend(decision.getAssessment().getRiskScore());
        message.setRiskLevelAtSend(decision.getAssessment().getRiskLevel().name());
        message.setStatus(decision.isCommunicationHold() ? MessageStatus.HELD : MessageStatus.LOCKED);
        message.setHoldReason(decision.isCommunicationHold() ? "ADMIN_REVIEW_REQUIRED" : null);
        message.setMetadata(packageData.getMetadata());
        message.setCreatedAt(LocalDateTime.now());

        Message saved = messageRepository.save(message);

        if (saved.getAlgorithmType() == AlgorithmType.CPHS) {
            CphsPuzzleFields fields = extractCphsFields(saved.getMetadata());
            PuzzleDifficulty diff = decision.getDifficulty();
            puzzleRepository.save(messagePuzzleService.buildPuzzleEntity(
                    saved,
                    fields.challenge,
                    fields.targetHash,
                    fields.maxIterations,
                    fields.wrappedKey,
                    diff.getAttemptsAllowed(),
                    diff.getTimeLimitSeconds()
            ));
        }
        auditService.record(
                AuditEventType.MESSAGE_SEND,
                senderUsername,
                receiver.getUsername(),
                ip,
                ua,
                decision.getAssessment().getRiskScore(),
                Map.of(
                        "requestedMode", decision.getRequestedMode().name(),
                        "effectiveMode", decision.getEffectiveMode().name(),
                        "escalated", decision.isEscalated(),
                        "hold", decision.isCommunicationHold(),
                        "reasons", decision.getReasons()
                )
        );

        LOGGER.info("Message {} sent from {} to {} requested={} effective={} hold={}",
                saved.getId(),
                senderUsername,
                receiver.getUsername(),
                decision.getRequestedMode(),
                decision.getEffectiveMode(),
                decision.isCommunicationHold()
        );

        MessageSendResponse response = new MessageSendResponse();
        response.setMessageId(saved.getId());
        response.setSenderUsername(saved.getSender().getUsername());
        response.setReceiverUsername(saved.getReceiver().getUsername());
        response.setRequestedAlgorithmType(decision.getRequestedMode());
        response.setEffectiveAlgorithmType(saved.getAlgorithmType());
        response.setEscalated(decision.isEscalated());
        response.setCommunicationHold(decision.isCommunicationHold());
        response.setRiskScore(decision.getAssessment().getRiskScore());
        response.setRiskLevel(decision.getAssessment().getRiskLevel().name());
        response.setEscalationReason(String.join(", ", decision.getReasons()));
        response.setCreatedAt(saved.getCreatedAt());
        response.setStatus(decision.isCommunicationHold()
                ? "Message held for admin-supervised recovery"
                : "Message stored securely");
        return response;
    }

    @Transactional(readOnly = true)
    public List<MessageSummaryResponse> getReceivedMessages(String receiverUsername) {
        User receiver = userService.getRequiredByUsername(receiverUsername);
        return messageRepository.findByReceiverOrderByCreatedAtDesc(receiver)
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Message getById(Long id) {
        if (id == null || id <= 0) {
            throw new BadRequestException("Message id must be positive");
        }
        return messageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + id));
    }

    @Transactional(readOnly = true)
    public Message getByIdForUser(Long id, String username) {
        if (id == null || id <= 0) {
            throw new BadRequestException("Message id must be positive");
        }
        User user = userService.getRequiredByUsername(username);
        return messageRepository.findByIdAndParticipant(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found for user: " + id));
    }

    @Transactional
    public MessageDecryptResponse decryptMessage(Long messageId, String receiverUsername) {
        if (messageId == null || messageId <= 0) {
            throw new BadRequestException("messageId must be positive");
        }
        User receiver = userService.getRequiredByUsername(receiverUsername);

        if (receiver.getRole() != Role.RECEIVER) {
            throw new BadRequestException("Only users with RECEIVER role can decrypt messages");
        }

        Message message = messageRepository.findByIdAndReceiver(messageId, receiver)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found for receiver: " + messageId));

        if (message.getStatus() == MessageStatus.HELD) {
            throw new BadRequestException("Communication is on hold: admin review required");
        }

        String plainText;
        long puzzleTimeMs = 0L;

        if (message.getAlgorithmType() == AlgorithmType.NORMAL) {
            plainText = cryptoService.decrypt(message.getEncryptedContent());
            message.setStatus(MessageStatus.UNLOCKED);
        } else if (message.getAlgorithmType() == AlgorithmType.SHCS) {
            plainText = shcsService.decryptAndExtract(message.getEncryptedContent());
            message.setStatus(MessageStatus.UNLOCKED);
        } else if (message.getAlgorithmType() == AlgorithmType.CPHS) {
            var puzzle = puzzleRepository.findByMessageId(message.getId())
                    .orElseThrow(() -> new BadRequestException("Puzzle is required for CPHS messages"));
            if (puzzle.getSolvedAt() == null || puzzle.getSolvedNonce() == null) {
                throw new BadRequestException("Puzzle must be solved before decryption");
            }
            CPHSDecryptionResult decryptionResult = cphsService.decryptAfterPuzzleNonce(
                    message.getEncryptedContent(),
                    message.getMetadata(),
                    puzzle.getSolvedNonce()
            );
            plainText = decryptionResult.getPlainText();
            puzzleTimeMs = decryptionResult.getPuzzleSolveTimeMs();
            message.setStatus(MessageStatus.UNLOCKED);
        } else {
            throw new BadRequestException("Unsupported algorithm type");
        }

        MessageDecryptResponse response = new MessageDecryptResponse();
        response.setMessageId(message.getId());
        response.setAlgorithmType(message.getAlgorithmType());
        response.setDecryptedContent(plainText);
        response.setPuzzleSolveTimeMs(puzzleTimeMs);
        response.setStatus("Decryption completed");

        messageRepository.save(message);
        LOGGER.info("Message {} decrypted by receiver {} using {}", messageId, receiverUsername, message.getAlgorithmType());
        return response;
    }

    private EncryptionPackage encryptByAlgorithm(
            AlgorithmType algorithmType,
            String plainText,
            String senderUsername,
            String receiverUsername,
            PuzzleDifficulty difficulty
    ) {
        return switch (algorithmType) {
            case NORMAL -> new EncryptionPackage(
                    cryptoService.encrypt(plainText),
                    normalMetadata()
            );
            case SHCS -> shcsService.encryptAndHideHeader(plainText, senderUsername, receiverUsername);
            case CPHS -> {
                Integer override = difficulty == null ? null : difficulty.getMaxIterations();
                yield override == null ? cphsService.encryptWithPuzzle(plainText) : cphsService.encryptWithPuzzle(plainText, override);
            }
        };
    }

    private MessageSummaryResponse toSummaryResponse(Message message) {
        MessageSummaryResponse response = new MessageSummaryResponse();
        response.setId(message.getId());
        response.setSenderUsername(message.getSender().getUsername());
        response.setReceiverUsername(message.getReceiver().getUsername());
        response.setEncryptedContent(message.getEncryptedContent());
        response.setAlgorithmType(message.getAlgorithmType());
        response.setRequestedAlgorithmType(message.getRequestedAlgorithmType());
        response.setStatus(message.getStatus() == null ? null : message.getStatus().name());
        response.setRiskScore(message.getRiskScoreAtSend());
        response.setRiskLevel(message.getRiskLevelAtSend());
        response.setWarning(buildReceiverWarning(message));
        response.setMetadata(message.getMetadata());
        response.setCreatedAt(message.getCreatedAt());
        return response;
    }

    private String buildReceiverWarning(Message message) {
        if (message.getStatus() == MessageStatus.HELD) {
            return "ADMIN_REVIEW_REQUIRED: communication is temporarily held due to elevated risk signals.";
        }
        if (message.getRiskLevelAtSend() == null) {
            return null;
        }
        if ("HIGH".equals(message.getRiskLevelAtSend()) || "CRITICAL".equals(message.getRiskLevelAtSend())) {
            return "ELEVATED_VERIFICATION: sender session risk was " + message.getRiskLevelAtSend() + " at send time.";
        }
        return null;
    }

    private String normalMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("profile", "NORMAL");
        metadata.put("visibility", "STANDARD");
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to build metadata", ex);
        }
    }

    private <T> void validateBean(T bean) {
        Set<ConstraintViolation<T>> violations = validator.validate(bean);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("; "));
            throw new BadRequestException(message);
        }
    }

    private String hashPlaintext(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return null;
        }
        // Deterministic integrity marker, without storing plaintext.
        return hashUtil.sha256Hex(plainText);
    }

    private CphsPuzzleFields extractCphsFields(String metadataJson) {
        try {
            Map<String, Object> metadata = objectMapper.readValue(metadataJson, new TypeReference<>() {});
            String challenge = requiredString(metadata, "challenge");
            String targetHash = requiredString(metadata, "targetHash");
            int maxIterations = requiredInteger(metadata, "maxIterations");
            String wrappedKey = requiredString(metadata, "wrappedKey");
            return new CphsPuzzleFields(challenge, targetHash, maxIterations, wrappedKey);
        } catch (Exception ex) {
            throw new BadRequestException("Invalid CPHS metadata format");
        }
    }

    private String requiredString(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new BadRequestException("Missing metadata field: " + key);
        }
        return value.toString();
    }

    private int requiredInteger(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value == null) {
            throw new BadRequestException("Missing metadata field: " + key);
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Invalid numeric metadata field: " + key);
        }
    }

    private record CphsPuzzleFields(String challenge, String targetHash, int maxIterations, String wrappedKey) {
    }
}
