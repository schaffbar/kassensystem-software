package de.schaffbar.core_pos.domain.rfid_tag_assignment_history;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentType;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.RfidTagId;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RfidTagAssignmentHistoryViews {

    RfidTagAssignmentHistoryViews MAPPER = Mappers.getMapper(RfidTagAssignmentHistoryViews.class);

    // ------------------------------------------------------------------------
    // mapper

    RfidTagAssignmentHistoryView toRfidTagAssignmentView(RfidTagAssignmentHistory rfidTagAssignment);

    // ------------------------------------------------------------------------
    // views

    @Builder
    @Validated
    record RfidTagAssignmentHistoryView( //
            @NotNull CustomerId customerId, //
            @NotNull RfidTagId rfidTagId, //
            @NotNull RfidTagAssignmentType assignmentType, //
            @NotNull Instant assignmentDate, //
            @NotNull Instant unassignmentDate //
    ) {}

}
