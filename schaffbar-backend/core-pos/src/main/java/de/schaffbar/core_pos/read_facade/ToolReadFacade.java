package de.schaffbar.core_pos.read_facade;

import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.tool.ToolService;
import de.schaffbar.core_pos.domain.tool.ToolViews.ToolView;
import de.schaffbar.core_pos.shared.cqrs.Query;
import de.schaffbar.core_pos.shared.id.RfidReaderId;
import de.schaffbar.core_pos.shared.id.ToolId;

@Service
@Validated
@RequiredArgsConstructor
public class ToolReadFacade {

    private final @NonNull ToolService toolService;

    @Query
    public List<ToolView> getTools() {
        return this.toolService.getTools();
    }

    @Query
    public Optional<ToolView> getTool(@NotNull @Valid ToolId toolId) {
        return this.toolService.getTool(toolId);
    }

    @Query
    public Optional<ToolView> getTool(@NotNull @Valid RfidReaderId rfidReaderId) {
        return this.toolService.getTool(rfidReaderId);
    }

}
