package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import de.schaffbar.core_pos.shared.id.RfidTagId;

public class RfidTagAlreadyRegisteredException extends BusinessRuleViolationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "RFID tag [id: %s] is already registered";

    public static final String CODE = "RFID_TAG_ALREADY_REGISTERED";

    public RfidTagAlreadyRegisteredException(RfidTagId rfidTagId) {
        super(CODE, String.format(MESSAGE, rfidTagId.getValue()));
    }

}
