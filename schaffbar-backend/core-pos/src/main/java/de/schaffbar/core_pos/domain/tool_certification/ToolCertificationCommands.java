package de.schaffbar.core_pos.domain.tool_certification;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;

public interface ToolCertificationCommands {

    @Builder
    @Validated
    record CreateToolCertificationCommand( //
            @Valid @NotNull CustomerId customerId, //
            @Valid @NotNull ToolId toolId, //
            @Valid CustomerId certifiedBy //
    ) {}

}
