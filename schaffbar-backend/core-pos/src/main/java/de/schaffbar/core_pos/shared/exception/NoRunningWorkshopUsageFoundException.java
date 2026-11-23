package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import de.schaffbar.core_pos.shared.id.CustomerId;

public class NoRunningWorkshopUsageFoundException extends BusinessRuleViolationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "No running workshop usage found for customer [id: %s]";

    public static final String CODE = "NO_RUNNING_WORKSHOP_USAGE_FOUND";

    public NoRunningWorkshopUsageFoundException(CustomerId customerId) {
        super(CODE, String.format(MESSAGE, customerId.getValue()));
    }

}
