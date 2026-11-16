package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.RfidTagId;

public class RfidTagAlreadyAssignedException extends BusinessRuleViolationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "RFID tag [id: %s] is already assigned to customer [id: %s]";

    public static final String CODE = "RFID_TAG_ALREADY_ASSIGNED";

    public RfidTagAlreadyAssignedException(RfidTagId rfidTagId, CustomerId customerId) {
        super(CODE, String.format(MESSAGE, rfidTagId.getValue(), customerId.getValue()));
    }

}
