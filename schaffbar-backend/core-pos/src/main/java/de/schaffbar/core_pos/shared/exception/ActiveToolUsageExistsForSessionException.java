package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import de.schaffbar.core_pos.shared.id.WorkshopSessionId;

public class ActiveToolUsageExistsForSessionException extends BusinessRuleViolationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "Active tool usage exists for workshop session [id: %s]";

    public static final String CODE = "ACTIVE_TOOL_USAGE_EXISTS_FOR_SESSION";

    public ActiveToolUsageExistsForSessionException(WorkshopSessionId workshopSessionId) {
        super(CODE, String.format(MESSAGE, workshopSessionId.getValue()));
    }

}
