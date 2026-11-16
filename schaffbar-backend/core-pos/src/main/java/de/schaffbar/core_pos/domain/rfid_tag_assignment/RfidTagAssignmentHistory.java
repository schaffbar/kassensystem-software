package de.schaffbar.core_pos.domain.rfid_tag_assignment;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.RfidTagId;

// inner entity of RfidTagAssignment: written by its unassignment, never changed
@Getter
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@Entity
@Table(name = "RFID_TAG_ASSIGNMENT_HISTORY", schema = "SCHAFFBAR")
class RfidTagAssignmentHistory {

    @Id
    private UUID id;

    @NotNull
    private UUID customerId;

    @NotNull
    private String rfidTagId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private RfidTagAssignmentType assignmentType;

    @NotNull
    @Past
    private Instant assignmentDate;

    @NotNull
    @PastOrPresent
    private Instant unassignmentDate;

    @Version
    private Instant updatedAt;

    // ------------------------------------------------------------------------
    // static constructor

    static RfidTagAssignmentHistory of(RfidTagAssignment assignment) {
        RfidTagAssignmentHistory result = new RfidTagAssignmentHistory();
        result.setId(UUID.randomUUID());
        result.setCustomerId(assignment.getCustomerId().getValue());
        result.setRfidTagId(assignment.getRfidTagId().getValue());
        result.setAssignmentType(assignment.getAssignmentType());
        result.setAssignmentDate(assignment.getAssignmentDate());
        result.setUnassignmentDate(Instant.now());

        return result;
    }

    // ------------------------------------------------------------------------
    // query

    public CustomerId getCustomerId() {
        return CustomerId.of(this.customerId);
    }

    public RfidTagId getRfidTagId() {
        return RfidTagId.of(this.rfidTagId);
    }

}
