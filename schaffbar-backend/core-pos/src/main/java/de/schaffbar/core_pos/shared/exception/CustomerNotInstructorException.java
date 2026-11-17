package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;

public class CustomerNotInstructorException extends BusinessRuleViolationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "Customer [id: %s] is not an instructor for tool [id: %s]";

    public static final String CODE = "CUSTOMER_NOT_INSTRUCTOR";

    public CustomerNotInstructorException(CustomerId customerId, ToolId toolId) {
        super(CODE, String.format(MESSAGE, customerId.getValue(), toolId.getValue()));
    }

}
