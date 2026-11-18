package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;

public class CustomerNotCertifiedForToolException extends BusinessRuleViolationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "Customer [id: %s] does not have an active certification for tool [id: %s]";

    public static final String CODE = "CUSTOMER_NOT_CERTIFIED_FOR_TOOL";

    public CustomerNotCertifiedForToolException(CustomerId customerId, ToolId toolId) {
        super(CODE, String.format(MESSAGE, customerId.getValue(), toolId.getValue()));
    }

}
