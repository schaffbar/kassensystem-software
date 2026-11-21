package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import de.schaffbar.core_pos.shared.id.RfidTagId;

public class NoCustomerAssignedException extends BusinessRuleViolationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "No customer assigned to RFID tag [id: %s]";

    public static final String CODE = "NO_CUSTOMER_ASSIGNED";

    public NoCustomerAssignedException(RfidTagId rfidTagId) {
        super(CODE, String.format(MESSAGE, rfidTagId.getValue()));
    }

}
