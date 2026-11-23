package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import de.schaffbar.core_pos.shared.id.WorkshopSessionId;

public class RunningWorkshopUsageExistsForSessionException extends BusinessRuleViolationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "A workshop usage is running in workshop session [id: %s]";

    public static final String CODE = "RUNNING_WORKSHOP_USAGE_EXISTS_FOR_SESSION";

    public RunningWorkshopUsageExistsForSessionException(WorkshopSessionId workshopSessionId) {
        super(CODE, String.format(MESSAGE, workshopSessionId.getValue()));
    }

}
