package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import de.schaffbar.core_pos.shared.id.ToolId;

public class ToolNameAlreadyUsedException extends BusinessRuleViolationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "Tool name '%s' is already used by tool [id: %s]";

    public static final String CODE = "TOOL_NAME_ALREADY_USED";

    public ToolNameAlreadyUsedException(String name, ToolId toolId) {
        super(CODE, String.format(MESSAGE, name, toolId.getValue()));
    }

}
