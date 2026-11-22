package de.schaffbar.core_pos.domain.rfid_reader;

import java.time.Instant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.shared.id.MacAddress;
import de.schaffbar.core_pos.shared.id.RfidReaderId;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RfidReaderViews {

    RfidReaderViews MAPPER = Mappers.getMapper(RfidReaderViews.class);

    // ------------------------------------------------------------------------
    // mapper

    RfidReaderView toRfidReaderView(RfidReader rfidReader);

    // ------------------------------------------------------------------------
    // views

    @Builder
    @Validated
    record RfidReaderView( //
            @NotNull @Valid RfidReaderId id, //
            @NotNull @Valid MacAddress macAddress, //
            RfidReaderType type, //
            String name, //
            String socketName, //
            @NotNull Instant createdAt //
    ) {}

}
