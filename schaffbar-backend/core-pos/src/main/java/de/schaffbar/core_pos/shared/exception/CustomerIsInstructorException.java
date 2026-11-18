package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;

public class CustomerIsInstructorException extends BusinessRuleViolationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "Customer [id: %s] is an instructor for tool [id: %s] and needs no certification";

    public static final String CODE = "CUSTOMER_IS_INSTRUCTOR";

    public CustomerIsInstructorException(CustomerId customerId, ToolId toolId) {
        super(CODE, String.format(MESSAGE, customerId.getValue(), toolId.getValue()));
    }

}
