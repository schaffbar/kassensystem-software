package de.schaffbar.core_pos.shared.event.payload;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import de.schaffbar.core_pos.shared.event.EventPayload;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.RfidTagAssignmentId;
import de.schaffbar.core_pos.shared.id.RfidTagId;

public interface RfidTagAssignmentEventPayload extends EventPayload {

    record RfidTagAssignmentRequestedPayload( //
            @Valid @NotNull RfidTagAssignmentId id, //
            @Valid @NotNull CustomerId customerId, //
            @NotBlank String assignmentType //
    ) implements RfidTagAssignmentEventPayload {}

    record RfidTagAssignmentCancelledPayload( //
            @Valid @NotNull RfidTagAssignmentId id, //
            @Valid @NotNull CustomerId customerId //
    ) implements RfidTagAssignmentEventPayload {}

    record RfidTagAssignedPayload( //
            @Valid @NotNull RfidTagAssignmentId id, //
            @Valid @NotNull CustomerId customerId, //
            @Valid @NotNull RfidTagId rfidTagId //
    ) implements RfidTagAssignmentEventPayload {}

    record RfidTagUnassignedPayload( //
            @Valid @NotNull RfidTagAssignmentId id, //
            @Valid @NotNull CustomerId customerId, //
            @Valid @NotNull RfidTagId rfidTagId //
    ) implements RfidTagAssignmentEventPayload {}

}
