package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import de.schaffbar.core_pos.shared.id.ToolId;

public class ToolAlreadyInUseByAnotherCustomerException extends BusinessRuleViolationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "Tool [id: %s] is already in use by another customer";

    public static final String CODE = "TOOL_ALREADY_IN_USE_BY_ANOTHER_CUSTOMER";

    public ToolAlreadyInUseByAnotherCustomerException(ToolId toolId) {
        super(CODE, String.format(MESSAGE, toolId.getValue()));
    }

}
