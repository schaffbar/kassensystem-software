package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.RfidTagId;

public class RfidTagAssignedToCustomerException extends BusinessRuleViolationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "Cannot delete RFID tag [id: %s] because it is assigned to customer [id: %s]. Please unassign it first.";

    public static final String CODE = "RFID_TAG_ASSIGNED_TO_CUSTOMER";

    public RfidTagAssignedToCustomerException(RfidTagId rfidTagId, CustomerId customerId) {
        super(CODE, String.format(MESSAGE, rfidTagId.getValue(), customerId.getValue()));
    }

}
