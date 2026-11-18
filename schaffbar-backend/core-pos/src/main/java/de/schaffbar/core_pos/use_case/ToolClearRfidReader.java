package de.schaffbar.core_pos.use_case;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.tool.ToolService;
import de.schaffbar.core_pos.domain.tool_usage.ToolUsageService;
import de.schaffbar.core_pos.shared.cqrs.Command;
import de.schaffbar.core_pos.shared.exception.ToolInUseException;
import de.schaffbar.core_pos.shared.id.ToolId;

@Service
@Validated
@RequiredArgsConstructor
public class ToolClearRfidReader {

    private final @NonNull ToolService toolService;

    private final @NonNull ToolUsageService toolUsageService;

    @Command
    public void process(@NotNull @Valid ToolId toolId) {
        ensureToolNotInUse(toolId);

        this.toolService.clearRfidReader(toolId);
    }

    // ------------------------------------------------------------------------
    // helper

    // the person at the tool could no longer stop the usage at its switch box (T-14)
    private void ensureToolNotInUse(ToolId toolId) {
        this.toolUsageService.getActiveToolUsage(toolId) //
                .ifPresent(_ -> throwToolInUseException(toolId));
    }

    private void throwToolInUseException(ToolId toolId) {
        throw ToolInUseException.clearRfidReader(toolId);
    }

}
