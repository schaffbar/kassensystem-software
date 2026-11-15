package de.schaffbar.core_pos.domain.rfid_reader;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import de.schaffbar.core_pos.shared.event.payload.RfidReaderEventPayload.RfidReaderCreatedPayload;
import de.schaffbar.core_pos.shared.event.payload.RfidReaderEventPayload.RfidReaderTypeChangedPayload;
import de.schaffbar.core_pos.shared.event.payload.RfidReaderEventPayload.RfidReaderUpdatedPayload;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface RfidReaderPayloadMapper {

    RfidReaderPayloadMapper MAPPER = Mappers.getMapper(RfidReaderPayloadMapper.class);

    // ------------------------------------------------------------------------
    // mapper

    RfidReaderCreatedPayload toRfidReaderCreatedPayload(RfidReader rfidReader);

    RfidReaderUpdatedPayload toRfidReaderUpdatedPayload(RfidReader rfidReader);

    RfidReaderTypeChangedPayload toRfidReaderTypeChangedPayload(RfidReader rfidReader);

}
