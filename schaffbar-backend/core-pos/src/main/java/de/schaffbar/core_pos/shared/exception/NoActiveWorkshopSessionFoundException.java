package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import de.schaffbar.core_pos.shared.id.CustomerId;

public class NoActiveWorkshopSessionFoundException extends BusinessRuleViolationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "No active workshop session found for customer [id: %s]";

    public static final String CODE = "NO_ACTIVE_WORKSHOP_SESSION_FOUND";

    public NoActiveWorkshopSessionFoundException(CustomerId id) {
        super(CODE, String.format(MESSAGE, id.getValue()));
    }

}
