package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import de.schaffbar.core_pos.shared.id.ToolCertificationId;

public class InvalidCertificationStateTransitionException extends BusinessRuleViolationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "Invalid state transition for certification [id: %s]: cannot transition from %s to %s";

    public static final String CODE = "INVALID_CERTIFICATION_STATE_TRANSITION";

    public InvalidCertificationStateTransitionException(ToolCertificationId id, String fromStatus, String toStatus) {
        super(CODE, String.format(MESSAGE, id.getValue(), fromStatus, toStatus));
    }

}
