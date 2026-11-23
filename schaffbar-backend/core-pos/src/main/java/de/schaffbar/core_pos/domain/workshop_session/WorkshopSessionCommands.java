package de.schaffbar.core_pos.domain.workshop_session;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.shared.billing.BillingTotal;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;

public interface WorkshopSessionCommands {

    @Builder
    @Validated
    record CloseWorkshopSessionCommand( //
            @NotNull @Valid WorkshopSessionId id, //
            @NotNull @Valid BillingTotal workshopTotal, //
            @NotNull @Valid BillingTotal toolTotal //
    ) {}

}
