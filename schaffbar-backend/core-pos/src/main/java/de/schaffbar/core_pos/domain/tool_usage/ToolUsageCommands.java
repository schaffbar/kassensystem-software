package de.schaffbar.core_pos.domain.tool_usage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;

public interface ToolUsageCommands {

    @Builder
    @Validated
    record StartToolUsageCommand( //
            @Valid @NotNull CustomerId customerId, //
            @Valid @NotNull ToolId toolId, //
            @Valid @NotNull WorkshopSessionId workshopSessionId //
    ) {}

}
