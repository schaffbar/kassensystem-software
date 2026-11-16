package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import de.schaffbar.core_pos.shared.id.CustomerId;

public class RfidTagAssignmentAlreadyWaitingException extends BusinessRuleViolationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "Cannot request an RFID tag for customer [id: %s] because the request of customer [id: %s] is still waiting for its tag";

    public static final String CODE = "RFID_TAG_ASSIGNMENT_ALREADY_WAITING";

    public RfidTagAssignmentAlreadyWaitingException(CustomerId customerId, CustomerId waitingCustomerId) {
        super(CODE, String.format(MESSAGE, customerId.getValue(), waitingCustomerId.getValue()));
    }

}
