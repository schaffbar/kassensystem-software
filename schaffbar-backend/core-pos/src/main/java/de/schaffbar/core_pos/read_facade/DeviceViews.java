package de.schaffbar.core_pos.read_facade;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import org.springframework.validation.annotation.Validated;

public interface DeviceViews {

    // what the gatekeeper shows after a check-in or check-out: the last stay of the open session
    @Builder
    @Validated
    record WorkshopStayView( //
            @PositiveOrZero long finishedUnits, // the units of the finished stays of the session
            @NotNull Instant entryTime, //
            Instant exitTime // null while the stay runs
    ) {}

}
