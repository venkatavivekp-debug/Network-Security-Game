package backend.service;

import backend.crypto.CPHSDecryptionResult;
import backend.crypto.EncryptionPackage;
import backend.dto.MessageDecryptResponse;
import backend.dto.MessageSendRequest;
import backend.dto.MessageSendResponse;
import backend.dto.MessageSummaryResponse;
import backend.exception.BadRequestException;
import backend.exception.ResourceNotFoundException;
import backend.model.AlgorithmType;
import backend.model.Message;
import backend.model.Role;
import backend.model.User;
import backend.repository.MessageRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public MessageService(
            MessageRepository messageRepository,
            UserService userService,
            CryptoService cryptoService,
            SHCSService shcsService,
            CPHSService cphsService,
            ObjectMapper objectMapper,
            Validator validator
    ) {
        this.messageRepository = messageRepository;
        this.userService = userService;
        this.cryptoService = cryptoService;
        this.shcsService = shcsService;
        this.cphsService = cphsService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Transactional
    public MessageSendResponse sendMessage(String senderUsername, MessageSendRequest request) {
        validateBean(request);

        User sender = userService.getRequiredByUsername(senderUsername);
        User receiver = userService.getRequiredByUsername(request.getReceiverUsername().trim());

        if (sender.getRole() != Role.SENDER) {
            throw new BadRequestException("Only users with SENDER role can send messages");
        }
        if (receiver.getRole() != Role.RECEIVER) {
            throw new BadRequestException("Messages can be sent only to users with RECEIVER role");
        }

        EncryptionPackage packageData = encryptByAlgorithm(
                request.getAlgorithmType(),
                request.getContent(),
                sender.getUsername(),
                receiver.getUsername()
        );

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setEncryptedContent(packageData.getEncryptedContent());
        message.setAlgorithmType(request.getAlgorithmType());
        message.setMetadata(packageData.getMetadata());
        message.setCreatedAt(LocalDateTime.now());

        Message saved = messageRepository.save(message);
        LOGGER.info("Message {} sent from {} to {} with {}", saved.getId(), senderUsername, receiver.getUsername(), saved.getAlgorithmType());

        MessageSendResponse response = new MessageSendResponse();
        response.setMessageId(saved.getId());
        response.setSenderUsername(saved.getSender().getUsername());
        response.setReceiverUsername(saved.getReceiver().getUsername());
        response.setAlgorithmType(saved.getAlgorithmType());
        response.setCreatedAt(saved.getCreatedAt());
        response.setStatus("Message stored securely");
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

        String plainText;
        long puzzleTimeMs = 0L;

        if (message.getAlgorithmType() == AlgorithmType.NORMAL) {
            plainText = cryptoService.decrypt(message.getEncryptedContent());
        } else if (message.getAlgorithmType() == AlgorithmType.SHCS) {
            plainText = shcsService.decryptAndExtract(message.getEncryptedContent());
        } else if (message.getAlgorithmType() == AlgorithmType.CPHS) {
            CPHSDecryptionResult decryptionResult = cphsService.decryptAfterPuzzleSolve(
                    message.getEncryptedContent(),
                    message.getMetadata()
            );
            plainText = decryptionResult.getPlainText();
            puzzleTimeMs = decryptionResult.getPuzzleSolveTimeMs();
        } else {
            throw new BadRequestException("Unsupported algorithm type");
        }

        MessageDecryptResponse response = new MessageDecryptResponse();
        response.setMessageId(message.getId());
        response.setAlgorithmType(message.getAlgorithmType());
        response.setDecryptedContent(plainText);
        response.setPuzzleSolveTimeMs(puzzleTimeMs);
        response.setStatus("Decryption completed");

        LOGGER.info("Message {} decrypted by receiver {} using {}", messageId, receiverUsername, message.getAlgorithmType());
        return response;
    }

    private EncryptionPackage encryptByAlgorithm(
            AlgorithmType algorithmType,
            String plainText,
            String senderUsername,
            String receiverUsername
    ) {
        return switch (algorithmType) {
            case NORMAL -> new EncryptionPackage(
                    cryptoService.encrypt(plainText),
                    normalMetadata()
            );
            case SHCS -> shcsService.encryptAndHideHeader(plainText, senderUsername, receiverUsername);
            case CPHS -> cphsService.encryptWithPuzzle(plainText);
        };
    }

    private MessageSummaryResponse toSummaryResponse(Message message) {
        MessageSummaryResponse response = new MessageSummaryResponse();
        response.setId(message.getId());
        response.setSenderUsername(message.getSender().getUsername());
        response.setReceiverUsername(message.getReceiver().getUsername());
        response.setEncryptedContent(message.getEncryptedContent());
        response.setAlgorithmType(message.getAlgorithmType());
        response.setMetadata(message.getMetadata());
        response.setCreatedAt(message.getCreatedAt());
        return response;
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
}
