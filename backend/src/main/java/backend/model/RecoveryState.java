package backend.model;

/**
 * Recovery state machine that the report and paper describe at a higher level
 * (attack -> defense -> recovery dynamic game). This enum gives every secured
 * communication an explicit, observable state so the frontend, admin and audit
 * trail can reason about it without exposing plaintext.
 *
 * <p>The states are intentionally non-terminal: every "blocked" state has a
 * controlled path back to NORMAL or RECOVERED.
 */
public enum RecoveryState {
    /** Default state for an unlocked / decrypted message. */
    NORMAL,

    /** A locked message that requires the receiver to complete its CPHS puzzle. */
    CHALLENGE_REQUIRED,

    /** Message went through risk-driven mode upgrade (e.g. NORMAL -> SHCS or SHCS -> CPHS). */
    ESCALATED,

    /** Communication is on hold and awaiting admin review. Plaintext is never released here. */
    HELD,

    /** Admin must explicitly review and release before the receiver can continue. */
    ADMIN_REVIEW_REQUIRED,

    /** Receiver is currently solving a recovery challenge or admin reset. */
    RECOVERY_IN_PROGRESS,

    /** Message was unlocked through the recovery path (post-hold or post-challenge). */
    RECOVERED,

    /** Message could not be unlocked: too many failures or expired puzzle. */
    FAILED
}
