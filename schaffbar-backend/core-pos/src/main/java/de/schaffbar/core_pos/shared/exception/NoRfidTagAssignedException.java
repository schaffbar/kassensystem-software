package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import de.schaffbar.core_pos.shared.id.CustomerId;

public class NoRfidTagAssignedException extends BusinessRuleViolationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "No RFID tag assigned to customer [id: %s]";

    public static final String CODE = "NO_RFID_TAG_ASSIGNED";

    public NoRfidTagAssignedException(CustomerId customerId) {
        super(CODE, String.format(MESSAGE, customerId.getValue()));
    }

}
