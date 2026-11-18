package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import de.schaffbar.core_pos.shared.id.CustomerId;

public class CustomerAlreadyInWorkshopException extends BusinessRuleViolationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "Customer [id: %s] is already in workshop";

    public static final String CODE = "CUSTOMER_ALREADY_IN_WORKSHOP";

    public CustomerAlreadyInWorkshopException(CustomerId customerId) {
        super(CODE, String.format(MESSAGE, customerId.getValue()));
    }

}
