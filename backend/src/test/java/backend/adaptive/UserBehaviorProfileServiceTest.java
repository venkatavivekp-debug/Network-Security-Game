package backend.adaptive;

import backend.model.User;
import backend.model.UserBehaviorProfile;
import backend.repository.UserBehaviorProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserBehaviorProfileServiceTest {

    private UserBehaviorProfileRepository repository;
    private UserBehaviorProfileService service;
    private final Map<Long, UserBehaviorProfile> store = new HashMap<>();

    @BeforeEach
    void setUp() {
        repository = mock(UserBehaviorProfileRepository.class);
        service = new UserBehaviorProfileService(repository);
        when(repository.findByUserId(any())).thenAnswer(inv -> Optional.ofNullable(store.get((Long) inv.getArgument(0))));
        when(repository.save(any(UserBehaviorProfile.class))).thenAnswer(inv -> {
            UserBehaviorProfile p = inv.getArgument(0);
            store.put(p.getUserId(), p);
            return p;
        });
    }

    private User user(long id) {
        User u = new User();
        u.setId(id);
        return u;
    }

    @Test
    void failureBurstShouldGrowThenDecayOverTime() {
        User u = user(7L);
        service.recordPuzzleFailure(u);
        service.recordPuzzleFailure(u);
        service.recordPuzzleFailure(u);

        UserBehaviorProfile profile = store.get(7L);
        assertEquals(3, profile.getConsecutiveFailures());

        // Pretend time has passed: rewind lastFailureAt by enough minutes for two decay steps.
        profile.setLastFailureAt(LocalDateTime.now().minusMinutes(UserBehaviorProfileService.DECAY_MINUTES * 2 + 1));
        store.put(7L, profile);

        int decayed = service.recentFailureBurst(u);
        assertTrue(decayed <= 1, "burst should decay over time, got " + decayed);
    }

    @Test
    void successShouldReduceConsecutiveFailures() {
        User u = user(8L);
        service.recordPuzzleFailure(u);
        service.recordPuzzleFailure(u);
        service.recordPuzzleSuccess(u, 1500);
        UserBehaviorProfile profile = store.get(8L);
        assertEquals(1, profile.getConsecutiveFailures());
        assertEquals(1, profile.getPuzzleSuccesses());
        assertEquals(2, profile.getPuzzleFailures());
        assertTrue(profile.getAvgSolveTimeMs() > 0);
    }

    @Test
    void recoveryEventShouldResetConsecutiveFailures() {
        User u = user(9L);
        service.recordPuzzleFailure(u);
        service.recordPuzzleFailure(u);
        service.recordRecoveryEvent(u);
        UserBehaviorProfile profile = store.get(9L);
        assertEquals(0, profile.getConsecutiveFailures());
        assertEquals(1, profile.getRecoveryEvents());
    }
}
