package de.schaffbar.core_pos.domain.rfid_tag;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import org.springframework.validation.annotation.Validated;

public interface RfidTagCommands {

    @Builder
    @Validated
    record CreateRfidTagCommand( //
            // TODO: switch to RfidTagId value object
            @NotBlank String rfidTagId  //
    ) {}

}
