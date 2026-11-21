package de.schaffbar.core_pos.read_facade;

import java.time.Instant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Builder;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;

public interface DashboardViews {

    @Builder
    @Validated
    record ActiveWorkshopUserView( //
            @Valid @NotNull CustomerId customerId, //
            @NotBlank String firstName, //
            @NotBlank String lastName, //
            @NotNull @PastOrPresent Instant entryTime //
    ) {}

    @Builder
    @Validated
    record ActiveToolUsageView( //
            @Valid @NotNull ToolId toolId, //
            @NotBlank String toolName, //
            @Valid @NotNull CustomerId customerId, //
            @NotBlank String firstName, //
            @NotBlank String lastName //
    ) {}

}
