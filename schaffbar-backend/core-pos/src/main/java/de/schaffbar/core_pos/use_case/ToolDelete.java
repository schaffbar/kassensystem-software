package de.schaffbar.core_pos.use_case;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.tool.ToolService;
import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationService;
import de.schaffbar.core_pos.domain.tool_usage.ToolUsageService;
import de.schaffbar.core_pos.shared.cqrs.Command;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.ToolInUseException;
import de.schaffbar.core_pos.shared.id.ToolId;

@Service
@Validated
@RequiredArgsConstructor
public class ToolDelete {

    private final @NonNull ToolService toolService;

    private final @NonNull ToolUsageService toolUsageService;

    private final @NonNull ToolCertificationService toolCertificationService;

    @Command
    public void process(@NotNull @Valid ToolId toolId) {
        this.toolService.getTool(toolId) //
                .orElseThrow(() -> ResourceNotFoundException.tool(toolId));

        // the usage of the person at the tool would never end (T-17)
        if (this.toolUsageService.getActiveToolUsage(toolId).isPresent()) {
            throw ToolInUseException.delete(toolId);
        }

        this.toolCertificationService.deleteAllByToolId(toolId);
        this.toolService.deleteTool(toolId);
    }

}
