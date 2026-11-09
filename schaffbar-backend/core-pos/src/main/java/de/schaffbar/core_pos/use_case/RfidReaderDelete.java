package de.schaffbar.core_pos.use_case;

import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderService;
import de.schaffbar.core_pos.domain.tool.ToolService;
import de.schaffbar.core_pos.shared.exception.RfidReaderHasToolAssignedException;
import de.schaffbar.core_pos.shared.id.RfidReaderId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class RfidReaderDelete {

    private final @NonNull RfidReaderService rfidReaderService;

    private final @NonNull ToolService toolService;

    @Transactional
    public void process(@NotNull @Valid RfidReaderId rfidReaderId) {
        verifyNoToolAssigned(rfidReaderId);

        this.rfidReaderService.deleteRfidReader(rfidReaderId);
    }

    // ------------------------------------------------------------------------
    // helper

    private void verifyNoToolAssigned(RfidReaderId rfidReaderId) {
        this.toolService.getTool(rfidReaderId) //
                .ifPresent(_ -> {
                    throw RfidReaderHasToolAssignedException.delete(rfidReaderId);
                });
    }

}
