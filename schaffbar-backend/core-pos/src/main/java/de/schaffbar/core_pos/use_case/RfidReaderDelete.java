package de.schaffbar.core_pos.use_case;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderService;
import de.schaffbar.core_pos.domain.tool.ToolService;
import de.schaffbar.core_pos.shared.cqrs.Command;
import de.schaffbar.core_pos.shared.exception.RfidReaderHasToolAssignedException;
import de.schaffbar.core_pos.shared.id.RfidReaderId;

@Service
@Validated
@RequiredArgsConstructor
public class RfidReaderDelete {

    private final @NonNull RfidReaderService rfidReaderService;

    private final @NonNull ToolService toolService;

    @Command
    public void process(@NotNull @Valid RfidReaderId rfidReaderId) {
        ensureNoToolAssigned(rfidReaderId);

        this.rfidReaderService.deleteRfidReader(rfidReaderId);
    }

    // ------------------------------------------------------------------------
    // helper

    // R-5
    private void ensureNoToolAssigned(RfidReaderId rfidReaderId) {
        this.toolService.getTool(rfidReaderId) //
                .ifPresent(_ -> throwRfidReaderHasToolAssignedException(rfidReaderId));
    }

    private void throwRfidReaderHasToolAssignedException(RfidReaderId rfidReaderId) {
        throw RfidReaderHasToolAssignedException.delete(rfidReaderId);
    }

}
