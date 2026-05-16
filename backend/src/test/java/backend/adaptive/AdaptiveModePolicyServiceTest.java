package backend.adaptive;

import backend.config.PuzzleProperties;
import backend.model.AlgorithmType;
import backend.model.Role;
import backend.model.User;
import backend.audit.AuditService;
import backend.util.HashUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AdaptiveModePolicyServiceTest {

    @Test
    void lowRiskShouldAllowRequestedMode() {
        AdaptiveSecurityProperties props = new AdaptiveSecurityProperties();
        ThreatSignalService threat = new ThreatSignalService();
        PuzzleProperties puzzleProps = new PuzzleProperties();
        puzzleProps.setMaxIterations(180000);
        puzzleProps.setChallengeBytes(12);
        puzzleProps.setSimulatedDelayMs(0);
        puzzleProps.setKeyDerivationSalt("test");

        AdaptiveSecurityService securityService = new AdaptiveSecurityService(props, new HashUtil(), mock(AuditService.class), threat);
        AdaptiveModePolicyService policy = new AdaptiveModePolicyService(securityService, threat, props, puzzleProps);

        User sender = new User("alice", "x", Role.SENDER);
        sender.setFailedLoginAttempts(0);
        AdaptiveDecision decision = policy.decide(sender, AlgorithmType.NORMAL, "1.2.3.4", "ua", 0, 0);

        assertEquals(AlgorithmType.NORMAL, decision.getRequestedMode());
        assertEquals(AlgorithmType.NORMAL, decision.getEffectiveMode());
        assertFalse(decision.isEscalated());
        assertFalse(decision.isCommunicationHold());
    }

    @Test
    void highRiskShouldEnforceCphs() {
        AdaptiveSecurityProperties props = new AdaptiveSecurityProperties();
        ThreatSignalService threat = new ThreatSignalService();
        threat.setAttackIntensity01(0.9);
        PuzzleProperties puzzleProps = new PuzzleProperties();
        puzzleProps.setMaxIterations(180000);
        puzzleProps.setChallengeBytes(12);
        puzzleProps.setSimulatedDelayMs(0);
        puzzleProps.setKeyDerivationSalt("test");

        AdaptiveSecurityService securityService = new AdaptiveSecurityService(props, new HashUtil(), mock(AuditService.class), threat);
        AdaptiveModePolicyService policy = new AdaptiveModePolicyService(securityService, threat, props, puzzleProps);

        User sender = new User("alice", "x", Role.SENDER);
        sender.setFailedLoginAttempts(8);
        AdaptiveDecision decision = policy.decide(sender, AlgorithmType.NORMAL, "1.2.3.4", "ua", 3, 2);

        assertEquals(AlgorithmType.CPHS, decision.getEffectiveMode());
        assertTrue(decision.isEscalated());
    }

    @Test
    void adaptiveModeShouldResolveToConcreteProtection() {
        AdaptiveSecurityProperties props = new AdaptiveSecurityProperties();
        ThreatSignalService threat = new ThreatSignalService();
        threat.setAttackIntensity01(0.65);
        PuzzleProperties puzzleProps = new PuzzleProperties();
        puzzleProps.setMaxIterations(180000);
        puzzleProps.setChallengeBytes(12);
        puzzleProps.setSimulatedDelayMs(0);
        puzzleProps.setKeyDerivationSalt("test");

        AdaptiveSecurityService securityService = new AdaptiveSecurityService(props, new HashUtil(), mock(AuditService.class), threat);
        AdaptiveModePolicyService policy = new AdaptiveModePolicyService(securityService, threat, props, puzzleProps);

        User sender = new User("alice", "x", Role.SENDER);
        sender.setFailedLoginAttempts(0);
        AdaptiveDecision decision = policy.decide(sender, AlgorithmType.ADAPTIVE, "1.2.3.4", "ua", 0, 0);

        assertEquals(AlgorithmType.ADAPTIVE, decision.getRequestedMode());
        assertEquals(AlgorithmType.SHCS, decision.getEffectiveMode());
        assertTrue(decision.isEscalated());
        assertTrue(decision.getReasons().contains("adaptive_selected_shcs"));
    }
}
