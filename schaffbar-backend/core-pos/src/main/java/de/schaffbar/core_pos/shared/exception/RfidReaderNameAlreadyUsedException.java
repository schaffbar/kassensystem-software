package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import de.schaffbar.core_pos.shared.id.RfidReaderId;

public class RfidReaderNameAlreadyUsedException extends BusinessRuleViolationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "RFID reader name '%s' is already used by RFID reader [id: %s]";

    public static final String CODE = "RFID_READER_NAME_ALREADY_USED";

    public RfidReaderNameAlreadyUsedException(String name, RfidReaderId rfidReaderId) {
        super(CODE, String.format(MESSAGE, name, rfidReaderId.getValue()));
    }

}
