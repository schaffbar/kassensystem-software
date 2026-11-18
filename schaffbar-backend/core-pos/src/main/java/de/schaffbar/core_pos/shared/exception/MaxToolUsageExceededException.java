package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import de.schaffbar.core_pos.shared.id.CustomerId;

public class MaxToolUsageExceededException extends BusinessRuleViolationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "Customer [id: %s] has reached the maximum number of simultaneously used tools (%d)";

    public static final String CODE = "MAX_TOOL_USAGE_EXCEEDED";

    public MaxToolUsageExceededException(CustomerId customerId, int maxToolUsages) {
        super(CODE, String.format(MESSAGE, customerId.getValue(), maxToolUsages));
    }

}
