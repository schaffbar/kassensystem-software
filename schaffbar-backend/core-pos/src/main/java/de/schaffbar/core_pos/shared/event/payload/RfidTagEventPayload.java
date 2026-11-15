package de.schaffbar.core_pos.shared.event.payload;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import de.schaffbar.core_pos.shared.event.EventPayload;
import de.schaffbar.core_pos.shared.id.RfidTagId;

public interface RfidTagEventPayload extends EventPayload {

    record RfidTagCreatedPayload( //
            @Valid @NotNull RfidTagId id //
    ) implements RfidTagEventPayload {}

    record RfidTagDeletedPayload( //
            @Valid @NotNull RfidTagId id //
    ) implements RfidTagEventPayload {}

}
