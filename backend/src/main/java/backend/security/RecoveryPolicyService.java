package backend.security;

import backend.model.RecoveryState;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Defines the recovery policy: for every {@link RecoveryState} the service exposes
 * a short list of legal next steps and a flag that says whether the state is a
 * stable terminal good state.
 *
 * <p>The goal is to make sure the API response and the UI never hand the user a
 * dead end: even {@code FAILED} (puzzle expired) ends in "ask the sender to
 * re-issue", which is a real path forward.
 */
@Service
public class RecoveryPolicyService {

    public record RecoveryPolicy(
            RecoveryState state,
            boolean terminalGood,
            String summary,
            List<String> nextSteps
    ) {
    }

    private static final Map<RecoveryState, RecoveryPolicy> POLICIES;

    static {
        Map<RecoveryState, RecoveryPolicy> map = new EnumMap<>(RecoveryState.class);

        map.put(RecoveryState.NORMAL, new RecoveryPolicy(
                RecoveryState.NORMAL,
                true,
                "Connection is stable.",
                List.of()
        ));

        map.put(RecoveryState.CHALLENGE_REQUIRED, new RecoveryPolicy(
                RecoveryState.CHALLENGE_REQUIRED,
                false,
                "Receiver must complete a CPHS challenge to unlock the message.",
                List.of(
                        "Open the challenge arena and submit a valid solution.",
                        "Remaining attempts are shown in the puzzle panel."
                )
        ));

        map.put(RecoveryState.ESCALATED, new RecoveryPolicy(
                RecoveryState.ESCALATED,
                false,
                "The adaptive engine escalated this message above the requested mode.",
                List.of(
                        "Solve the stronger challenge if one was issued.",
                        "Risk score will fall after a clean session."
                )
        ));

        map.put(RecoveryState.HELD, new RecoveryPolicy(
                RecoveryState.HELD,
                false,
                "Communication is on hold and awaiting admin review.",
                List.of(
                        "Admin must release the message from the SOC console.",
                        "Receiver can attempt the puzzle again only after release."
                )
        ));

        map.put(RecoveryState.ADMIN_REVIEW_REQUIRED, new RecoveryPolicy(
                RecoveryState.ADMIN_REVIEW_REQUIRED,
                false,
                "Puzzle attempts exhausted. Admin review required.",
                List.of(
                        "Admin can reset failure counters or release the message.",
                        "Receiver must wait for admin action before retrying."
                )
        ));

        map.put(RecoveryState.RECOVERY_IN_PROGRESS, new RecoveryPolicy(
                RecoveryState.RECOVERY_IN_PROGRESS,
                false,
                "Receiver solved the challenge; finalizing decryption.",
                List.of(
                        "Decrypt to complete the recovery.",
                        "Audit log will record the unlock event."
                )
        ));

        map.put(RecoveryState.RECOVERED, new RecoveryPolicy(
                RecoveryState.RECOVERED,
                true,
                "Message recovered after escalation.",
                List.of()
        ));

        map.put(RecoveryState.FAILED, new RecoveryPolicy(
                RecoveryState.FAILED,
                false,
                "Challenge window expired. Original delivery failed.",
                List.of(
                        "Sender can re-issue the message with a fresh challenge.",
                        "Admin may release if the message was held during the window."
                )
        ));

        POLICIES = Map.copyOf(map);
    }

    public RecoveryPolicy policyFor(RecoveryState state) {
        if (state == null) {
            return POLICIES.get(RecoveryState.NORMAL);
        }
        return POLICIES.get(state);
    }

    public List<String> nextSteps(RecoveryState state) {
        return policyFor(state).nextSteps();
    }

    public Map<RecoveryState, RecoveryPolicy> all() {
        return POLICIES;
    }
}
