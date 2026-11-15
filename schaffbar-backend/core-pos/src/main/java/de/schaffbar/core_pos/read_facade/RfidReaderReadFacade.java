package de.schaffbar.core_pos.read_facade;

import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderService;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderViews.RfidReaderView;
import de.schaffbar.core_pos.shared.cqrs.Query;
import de.schaffbar.core_pos.shared.id.MacAddress;
import de.schaffbar.core_pos.shared.id.RfidReaderId;

@Service
@Validated
@RequiredArgsConstructor
public class RfidReaderReadFacade {

    private final @NonNull RfidReaderService rfidReaderService;

    @Query
    public List<RfidReaderView> getRfidReaders() {
        return this.rfidReaderService.getRfidReaders();
    }

    @Query
    public Optional<RfidReaderView> getRfidReader(@NotNull @Valid RfidReaderId rfidReaderId) {
        return this.rfidReaderService.getRfidReader(rfidReaderId);
    }

    @Query
    public Optional<RfidReaderView> getRfidReader(@NotNull @Valid MacAddress macAddress) {
        return this.rfidReaderService.getRfidReader(macAddress);
    }

}
