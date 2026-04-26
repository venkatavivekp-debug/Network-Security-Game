package backend.adaptive;

import backend.model.AlgorithmType;
import backend.model.Message;
import backend.model.MessageStatus;
import backend.model.Puzzle;
import backend.model.RecoveryState;

import java.time.LocalDateTime;

/**
 * Translates the storage-level {@link MessageStatus} (LOCKED / UNLOCKED / HELD)
 * into the richer {@link RecoveryState} the rest of the system reports.
 *
 * <p>Storage stays small and stable; the API surface uses the named recovery
 * states described in the project report and paper.
 */
public final class RecoveryStateMapper {

    private RecoveryStateMapper() {
    }

    /**
     * Decide the recovery state for a message based on its status, the
     * algorithm it ended up using, and (optionally) its CPHS puzzle.
     *
     * @param message the message; required
     * @param puzzle  the associated CPHS puzzle if any; may be null
     */
    public static RecoveryState resolve(Message message, Puzzle puzzle) {
        if (message == null || message.getStatus() == null) {
            return RecoveryState.NORMAL;
        }

        MessageStatus status = message.getStatus();
        if (status == MessageStatus.HELD) {
            return RecoveryState.HELD;
        }
        if (status == MessageStatus.UNLOCKED) {
            return wasEscalated(message) ? RecoveryState.RECOVERED : RecoveryState.NORMAL;
        }

        // status == LOCKED
        if (message.getAlgorithmType() == AlgorithmType.CPHS) {
            if (puzzle == null) {
                return RecoveryState.CHALLENGE_REQUIRED;
            }
            if (puzzle.getSolvedAt() != null) {
                return RecoveryState.RECOVERY_IN_PROGRESS;
            }
            if (puzzle.getExpiresAt() != null && LocalDateTime.now().isAfter(puzzle.getExpiresAt())) {
                return RecoveryState.FAILED;
            }
            if (puzzle.getAttemptsUsed() >= puzzle.getAttemptsAllowed()) {
                return RecoveryState.ADMIN_REVIEW_REQUIRED;
            }
            return RecoveryState.CHALLENGE_REQUIRED;
        }
        return wasEscalated(message) ? RecoveryState.ESCALATED : RecoveryState.NORMAL;
    }

    /**
     * Convenience overload when the puzzle is not at hand. The receiver inbox
     * uses this when puzzles are not eagerly fetched.
     */
    public static RecoveryState resolve(Message message) {
        return resolve(message, null);
    }

    private static boolean wasEscalated(Message message) {
        AlgorithmType requested = message.getRequestedAlgorithmType();
        AlgorithmType effective = message.getAlgorithmType();
        if (requested == null || effective == null) {
            return false;
        }
        return requested != effective;
    }
}
