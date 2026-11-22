package de.schaffbar.core_pos.domain.rfid_tag_assignment.web;

import java.time.Instant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentStatus;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentType;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentViews.RfidTagAssignmentHistoryView;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentViews.RfidTagAssignmentView;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.RfidTagId;
import de.schaffbar.core_pos.shared.id.ValueObjectMapper;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RfidTagAssignmentApiModel extends ValueObjectMapper {

    RfidTagAssignmentApiModel MAPPER = Mappers.getMapper(RfidTagAssignmentApiModel.class);

    // ------------------------------------------------------------------------
    // mapping view to response

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "customerId", source = "customerId.value")
    @Mapping(target = "rfidTagId", source = "rfidTagId.value")
    RfidTagAssignmentApiDto toRfidTagAssignmentApiDto(RfidTagAssignmentView rfidTagAssignment);

    @Mapping(target = "customerId", source = "customerId.value")
    @Mapping(target = "rfidTagId", source = "rfidTagId.value")
    RfidTagAssignmentHistoryApiDto toRfidTagAssignmentHistoryApiDto(RfidTagAssignmentHistoryView historyEntry);

    // ------------------------------------------------------------------------
    // response

    record RfidTagAssignmentApiDto( //
            @NotBlank String id, //
            @NotBlank String customerId, //
            @NotNull RfidTagAssignmentType assignmentType, //
            @NotNull RfidTagAssignmentStatus status, //
            String rfidTagId, //
            Instant assignmentDate //
    ) {}

    record RfidTagAssignmentHistoryApiDto( //
            @NotBlank String customerId, //
            @NotBlank String rfidTagId, //
            @NotNull RfidTagAssignmentType assignmentType, //
            @NotNull Instant assignmentDate, //
            @NotNull Instant unassignmentDate //
    ) {}

    // ------------------------------------------------------------------------
    // request body

    record RequestRfidTagAssignmentRequestBody( //
            @NotNull @Valid CustomerId customerId, //
            @NotNull RfidTagAssignmentType assignmentType //
    ) {}

    record AssignRfidTagRequestBody( //
            @NotNull @Valid RfidTagId rfidTagId //
    ) {}

    record UnassignRfidTagRequestBody( //
            @NotNull @Valid CustomerId customerId //
    ) {}

}
