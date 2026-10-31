package de.schaffbar.core_pos.domain.rfid_tag_assignment;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.RfidTagAssignmentId;
import de.schaffbar.core_pos.shared.id.RfidTagId;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RfidTagAssignmentViews {

    RfidTagAssignmentViews MAPPER = Mappers.getMapper(RfidTagAssignmentViews.class);

    // ------------------------------------------------------------------------
    // mapper

    RfidTagAssignmentView toRfidTagAssignmentView(RfidTagAssignment rfidTagAssignment);

    // ------------------------------------------------------------------------
    // views

    @Builder
    @Validated
    record RfidTagAssignmentView( //
            @NotNull RfidTagAssignmentId id, //
            @NotNull CustomerId customerId, //
            @NotNull RfidTagAssignmentType assignmentType, //
            @NotNull RfidTagAssignmentStatus status, //
            RfidTagId rfidTagId, //
            Instant assignmentDate //
    ) {}

}
