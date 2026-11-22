package de.schaffbar.core_pos.domain.rfid_tag_assignment;

import static java.util.Objects.isNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.RfidTagAssignmentId;
import de.schaffbar.core_pos.shared.id.RfidTagId;

@Getter
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@Entity
@Table(name = "RFID_TAG_ASSIGNMENT", schema = "SCHAFFBAR")
class RfidTagAssignment {

    @Id
    private UUID id;

    @NotNull
    @Column(unique = true)
    private UUID customerId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private RfidTagAssignmentType assignmentType;

    @NotNull
    @Enumerated(EnumType.STRING)
    private RfidTagAssignmentStatus status;

    @Column(unique = true)
    private String rfidTagId;

    private Instant assignmentDate;

    @Version
    private Instant updatedAt;

    // ------------------------------------------------------------------------
    // static constructor

    public static RfidTagAssignment of(CustomerId customerId, RfidTagAssignmentType assignmentType) {
        RfidTagAssignment result = new RfidTagAssignment();
        result.setId(RfidTagAssignmentId.random().getValue());
        result.setCustomerId(customerId.getValue());
        result.setAssignmentType(assignmentType);
        result.setStatus(RfidTagAssignmentStatus.WAITING_FOR_ASSIGNMENT);

        return result;
    }

    // ------------------------------------------------------------------------
    // query

    public RfidTagAssignmentId getId() {
        return RfidTagAssignmentId.of(this.id);
    }

    public CustomerId getCustomerId() {
        return CustomerId.of(this.customerId);
    }

    public RfidTagId getRfidTagId() {
        if (isNull(this.rfidTagId)) {
            return null;
        }

        return RfidTagId.of(this.rfidTagId);
    }

    public boolean isAssigned() {
        return RfidTagAssignmentStatus.ASSIGNED == this.status;
    }

    public boolean isWaitingForAssignment() {
        return RfidTagAssignmentStatus.WAITING_FOR_ASSIGNMENT == this.status;
    }

    // ------------------------------------------------------------------------
    // command

    public List<SchaffbarEvent> assignRfidTag(RfidTagId rfidTagId) {
        // the service finds only the waiting request (TA-4)
        if (!isWaitingForAssignment()) {
            throw new IllegalStateException("RFID tag assignment [id: " + this.id + "] is not waiting for a tag");
        }

        setRfidTagId(rfidTagId.getValue());
        setStatus(RfidTagAssignmentStatus.ASSIGNED);
        setAssignmentDate(Instant.now());

        return List.of(RfidTagAssignmentEventFactory.rfidTagAssigned(this));
    }

    public List<SchaffbarEvent> unassignRfidTag(RfidTagAssignmentHistory historyEntry) {
        // the service finds only an assigned one (TA-5)
        if (!isAssigned()) {
            throw new IllegalStateException("RFID tag assignment [id: " + this.id + "] has no tag to unassign");
        }

        return List.of(RfidTagAssignmentEventFactory.rfidTagUnassigned(this, historyEntry));
    }

    public List<SchaffbarEvent> cancel() {
        // the service finds only a waiting request (TA-10)
        if (!isWaitingForAssignment()) {
            throw new IllegalStateException("RFID tag assignment [id: " + this.id + "] is not waiting for a tag");
        }

        return List.of(RfidTagAssignmentEventFactory.rfidTagAssignmentCancelled(this));
    }

}
