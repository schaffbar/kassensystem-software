package de.schaffbar.core_pos.domain.rfid_reader;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.shared.id.RfidReaderId;

public interface RfidReaderCommands {

    @Builder
    @Validated
    record UpdateRfidReaderCommand( //
            @NotNull @Valid RfidReaderId id, //
            String name, //
            String socketName //
    ) {}

    @Builder
    @Validated
    record ChangeRfidReaderTypeCommand( //
            @NotNull @Valid RfidReaderId id, //
            @NotNull RfidReaderType type //
    ) {}

}
