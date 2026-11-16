package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.RfidTagId;

public class NoRfidTagAssignmentWaitingException extends BusinessRuleViolationException {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String CODE = "NO_RFID_TAG_ASSIGNMENT_WAITING";

    private NoRfidTagAssignmentWaitingException(String message) {
        super(CODE, message);
    }

    // ------------------------------------------------------------------------
    // static factories

    public static NoRfidTagAssignmentWaitingException forRfidTag(RfidTagId rfidTagId) {
        return new NoRfidTagAssignmentWaitingException(String.format("Cannot assign RFID tag [id: %s] because no request is waiting for a tag", rfidTagId.getValue()));
    }

    public static NoRfidTagAssignmentWaitingException forCustomer(CustomerId customerId) {
        return new NoRfidTagAssignmentWaitingException(String.format("No request of customer [id: %s] is waiting for a tag", customerId.getValue()));
    }

}
