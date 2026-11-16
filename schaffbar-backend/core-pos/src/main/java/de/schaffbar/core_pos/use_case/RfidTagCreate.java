package de.schaffbar.core_pos.use_case;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.rfid_tag.RfidTagService;
import de.schaffbar.core_pos.shared.cqrs.Command;
import de.schaffbar.core_pos.shared.id.RfidTagId;

@Service
@Validated
@RequiredArgsConstructor
public class RfidTagCreate {

    private final @NonNull RfidTagService rfidTagService;

    @Command
    public RfidTagId process(@NotNull @Valid RfidTagId rfidTagId) {
        return this.rfidTagService.createRfidTag(rfidTagId);
    }

}
