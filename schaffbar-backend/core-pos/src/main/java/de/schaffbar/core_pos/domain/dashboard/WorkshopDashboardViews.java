package de.schaffbar.core_pos.domain.dashboard;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Builder;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.shared.id.CustomerId;

public interface WorkshopDashboardViews {

    // ------------------------------------------------------------------------
    // views

    @Builder
    @Validated
    record WorkshopDashboardEntryView( //
            @NotNull CustomerId customerId, //
            @NotBlank String firstName, //
            @NotBlank String lastName, //
            @NotNull @PastOrPresent Instant entryTime //
    ) {}

}
