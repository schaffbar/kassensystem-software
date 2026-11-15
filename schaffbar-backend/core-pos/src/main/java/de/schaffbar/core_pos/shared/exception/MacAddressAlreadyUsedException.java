package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import de.schaffbar.core_pos.shared.id.MacAddress;
import de.schaffbar.core_pos.shared.id.RfidReaderId;

public class MacAddressAlreadyUsedException extends BusinessRuleViolationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "MAC address '%s' is already used by RFID reader [id: %s]";

    public static final String CODE = "MAC_ADDRESS_ALREADY_USED";

    public MacAddressAlreadyUsedException(MacAddress macAddress, RfidReaderId rfidReaderId) {
        super(CODE, String.format(MESSAGE, macAddress.getValue(), rfidReaderId.getValue()));
    }

}
