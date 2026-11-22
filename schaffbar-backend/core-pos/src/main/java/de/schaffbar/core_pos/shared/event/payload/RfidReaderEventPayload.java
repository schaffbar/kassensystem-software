package de.schaffbar.core_pos.shared.event.payload;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType;
import de.schaffbar.core_pos.shared.event.EventPayload;
import de.schaffbar.core_pos.shared.id.MacAddress;
import de.schaffbar.core_pos.shared.id.RfidReaderId;

public interface RfidReaderEventPayload extends EventPayload {

    record RfidReaderCreatedPayload( //
            @Valid @NotNull RfidReaderId id, //
            @Valid @NotNull MacAddress macAddress //
    ) implements RfidReaderEventPayload {}

    record RfidReaderUpdatedPayload( //
            @Valid @NotNull RfidReaderId id, //
            RfidReaderType type, //
            String name, //
            String socketName //
    ) implements RfidReaderEventPayload {}

    record RfidReaderTypeChangedPayload( //
            @Valid @NotNull RfidReaderId id, //
            @NotNull RfidReaderType type //
    ) implements RfidReaderEventPayload {}

    record RfidReaderDeletedPayload( //
            @Valid @NotNull RfidReaderId id //
    ) implements RfidReaderEventPayload {}

}
