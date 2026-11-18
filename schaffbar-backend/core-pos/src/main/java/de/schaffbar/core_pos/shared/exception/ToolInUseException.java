package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import de.schaffbar.core_pos.shared.id.ToolId;

public class ToolInUseException extends BusinessRuleViolationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "Cannot %s tool [id: %s] because somebody is using it. The usage has to end first.";

    public static final String CODE = "TOOL_IN_USE";

    private ToolInUseException(ToolId id, String action) {
        super(CODE, String.format(MESSAGE, action, id.getValue()));
    }

    // ------------------------------------------------------------------------
    // static factories

    public static ToolInUseException clearRfidReader(ToolId id) {
        return new ToolInUseException(id, "clear the RFID reader of");
    }

    public static ToolInUseException delete(ToolId id) {
        return new ToolInUseException(id, "delete");
    }

}
