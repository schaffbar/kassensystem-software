package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import de.schaffbar.core_pos.shared.id.WorkshopSessionId;

public class ActiveWorkshopUsageExistsForSessionException extends BusinessRuleViolationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "Active workshop usage exists for workshop session [id: %s]";

    public static final String CODE = "ACTIVE_WORKSHOP_USAGE_EXISTS_FOR_SESSION";

    public ActiveWorkshopUsageExistsForSessionException(WorkshopSessionId workshopSessionId) {
        super(CODE, String.format(MESSAGE, workshopSessionId.getValue()));
    }

}
