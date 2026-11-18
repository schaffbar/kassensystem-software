package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import de.schaffbar.core_pos.shared.id.CustomerId;

public class NoOpenWorkshopSessionException extends BusinessRuleViolationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "Customer [id: %s] has no open workshop session";

    public static final String CODE = "NO_OPEN_WORKSHOP_SESSION";

    public NoOpenWorkshopSessionException(CustomerId customerId) {
        super(CODE, String.format(MESSAGE, customerId.getValue()));
    }

}
