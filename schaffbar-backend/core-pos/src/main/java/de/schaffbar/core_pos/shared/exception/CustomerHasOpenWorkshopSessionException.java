package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import de.schaffbar.core_pos.shared.id.CustomerId;

public class CustomerHasOpenWorkshopSessionException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "Cannot delete customer [id: %s] because the workshop session is still open. Please close the session first before deleting the customer.";

    public CustomerHasOpenWorkshopSessionException(CustomerId id) {
        super(String.format(MESSAGE, id.getValue()), null);
    }

}
