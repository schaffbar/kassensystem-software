package de.schaffbar.core_pos.use_case;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.tool_usage.ToolUsageService;
import de.schaffbar.core_pos.shared.cqrs.Command;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;

@Service
@Validated
@RequiredArgsConstructor
public class ToolUsageToggle {

    private final @NonNull ToolUsageService toolUsageService;

    private final @NonNull ToolUsageStart toolUsageStart;

    private final @NonNull ToolUsageStop toolUsageStop;

    // the switch box has one action: holding the tag switches the tool off when the person uses it, else on (TU-11)
    @Command
    public void process(@NotNull @Valid CustomerId customerId, @NotNull @Valid ToolId toolId) {
        if (this.toolUsageService.getRunningToolUsage(customerId, toolId).isPresent()) {
            this.toolUsageStop.process(customerId, toolId);
        }
        else {
            this.toolUsageStart.process(customerId, toolId);
        }
    }

}
