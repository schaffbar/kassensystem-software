package de.schaffbar.core_pos.domain.rfid_tag_assignment;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import de.schaffbar.core_pos.shared.event.payload.RfidTagAssignmentEventPayload.RfidTagAssignedPayload;
import de.schaffbar.core_pos.shared.event.payload.RfidTagAssignmentEventPayload.RfidTagAssignmentCancelledPayload;
import de.schaffbar.core_pos.shared.event.payload.RfidTagAssignmentEventPayload.RfidTagAssignmentRequestedPayload;
import de.schaffbar.core_pos.shared.event.payload.RfidTagAssignmentEventPayload.RfidTagUnassignedPayload;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface RfidTagAssignmentPayloadMapper {

    RfidTagAssignmentPayloadMapper MAPPER = Mappers.getMapper(RfidTagAssignmentPayloadMapper.class);

    // ------------------------------------------------------------------------
    // mapper

    RfidTagAssignmentRequestedPayload toRfidTagAssignmentRequestedPayload(RfidTagAssignment assignment);

    RfidTagAssignmentCancelledPayload toRfidTagAssignmentCancelledPayload(RfidTagAssignment assignment);

    RfidTagAssignedPayload toRfidTagAssignedPayload(RfidTagAssignment assignment);

    RfidTagUnassignedPayload toRfidTagUnassignedPayload(RfidTagAssignment assignment);

}
