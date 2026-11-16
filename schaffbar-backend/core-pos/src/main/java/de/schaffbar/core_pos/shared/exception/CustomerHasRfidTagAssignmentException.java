package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import de.schaffbar.core_pos.shared.id.CustomerId;

public class CustomerHasRfidTagAssignmentException extends BusinessRuleViolationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "Customer [id: %s] already has an RFID tag or a waiting request for one";

    public static final String CODE = "CUSTOMER_HAS_RFID_TAG_ASSIGNMENT";

    public CustomerHasRfidTagAssignmentException(CustomerId customerId) {
        super(CODE, String.format(MESSAGE, customerId.getValue()));
    }

}
