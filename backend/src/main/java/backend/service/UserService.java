package backend.service;

import backend.dto.AuthResponse;
import backend.dto.RegisterRequest;
import backend.exception.BadRequestException;
import backend.exception.ResourceNotFoundException;
import backend.model.User;
import backend.repository.UserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, Validator validator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        validateBean(request);

        String normalizedUsername = request.getUsername().trim();
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new BadRequestException("Username already exists");
        }

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());

        User saved = userRepository.save(user);
        LOGGER.info("Registered new user '{}' with role {}", saved.getUsername(), saved.getRole());

        return new AuthResponse(saved.getUsername(), saved.getRole(), "Registration successful");
    }

    @Transactional(readOnly = true)
    public User getRequiredByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
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
