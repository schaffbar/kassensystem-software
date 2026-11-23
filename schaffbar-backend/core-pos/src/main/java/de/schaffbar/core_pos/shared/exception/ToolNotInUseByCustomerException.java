package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;

public class ToolNotInUseByCustomerException extends BusinessRuleViolationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "No running tool usage found for customer [id: %s] and tool [id: %s]";

    public static final String CODE = "TOOL_NOT_IN_USE_BY_CUSTOMER";

    public ToolNotInUseByCustomerException(CustomerId customerId, ToolId toolId) {
        super(CODE, String.format(MESSAGE, customerId.getValue(), toolId.getValue()));
    }

}
