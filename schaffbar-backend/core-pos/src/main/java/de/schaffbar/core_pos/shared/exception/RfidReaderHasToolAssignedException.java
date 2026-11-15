package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import de.schaffbar.core_pos.shared.id.RfidReaderId;

public class RfidReaderHasToolAssignedException extends BusinessRuleViolationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "Cannot %s RFID reader [id: %s] because it is currently assigned to a tool. Please unassign the tool first before %s.";

    public static final String CODE = "RFID_READER_HAS_TOOL_ASSIGNED";

    private RfidReaderHasToolAssignedException(RfidReaderId id, String action, String actionGerund) {
        super(CODE, String.format(MESSAGE, action, id.getValue(), actionGerund));
    }

    // ------------------------------------------------------------------------
    // static factories

    public static RfidReaderHasToolAssignedException changeType(RfidReaderId id) {
        return new RfidReaderHasToolAssignedException(id, "change type of", "changing the type");
    }

    public static RfidReaderHasToolAssignedException delete(RfidReaderId id) {
        return new RfidReaderHasToolAssignedException(id, "delete", "deleting it");
    }

}
