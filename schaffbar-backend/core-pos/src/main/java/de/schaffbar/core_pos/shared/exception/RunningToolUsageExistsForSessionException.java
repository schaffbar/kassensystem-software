package de.schaffbar.core_pos.shared.exception;

import java.io.Serial;

import de.schaffbar.core_pos.shared.id.WorkshopSessionId;

public class RunningToolUsageExistsForSessionException extends BusinessRuleViolationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String MESSAGE = "A tool usage is running in workshop session [id: %s]";

    public static final String CODE = "RUNNING_TOOL_USAGE_EXISTS_FOR_SESSION";

    public RunningToolUsageExistsForSessionException(WorkshopSessionId workshopSessionId) {
        super(CODE, String.format(MESSAGE, workshopSessionId.getValue()));
    }

}
