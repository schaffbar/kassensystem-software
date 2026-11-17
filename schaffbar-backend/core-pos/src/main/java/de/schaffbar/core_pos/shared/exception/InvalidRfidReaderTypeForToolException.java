package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import de.schaffbar.core_pos.shared.id.RfidReaderId;

public class InvalidRfidReaderTypeForToolException extends BusinessRuleViolationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "RFID reader [id: %s] has an invalid type for tool assignment. Only SWITCH_BOX readers can be assigned to tools.";

    public static final String CODE = "INVALID_RFID_READER_TYPE_FOR_TOOL";

    public InvalidRfidReaderTypeForToolException(RfidReaderId rfidReaderId) {
        super(CODE, String.format(MESSAGE, rfidReaderId.getValue()));
    }

}
