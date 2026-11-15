package de.schaffbar.core_pos.domain.rfid_tag;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import de.schaffbar.core_pos.shared.event.payload.RfidTagEventPayload.RfidTagCreatedPayload;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface RfidTagPayloadMapper {

    RfidTagPayloadMapper MAPPER = Mappers.getMapper(RfidTagPayloadMapper.class);

    // ------------------------------------------------------------------------
    // mapper

    RfidTagCreatedPayload toRfidTagCreatedPayload(RfidTag rfidTag);

}
