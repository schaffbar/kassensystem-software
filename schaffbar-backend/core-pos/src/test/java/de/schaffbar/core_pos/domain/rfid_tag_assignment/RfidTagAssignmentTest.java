package de.schaffbar.core_pos.domain.rfid_tag_assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.schaffbar.core_pos.builder.RandomValues;
import de.schaffbar.core_pos.shared.event.EventType;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.event.payload.RfidTagAssignmentEventPayload.RfidTagAssignedPayload;
import de.schaffbar.core_pos.shared.event.payload.RfidTagAssignmentEventPayload.RfidTagAssignmentCancelledPayload;
import de.schaffbar.core_pos.shared.event.payload.RfidTagAssignmentEventPayload.RfidTagUnassignedPayload;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.RfidTagId;

class RfidTagAssignmentTest {

    // ------------------------------------------------------------------------
    // of

    @Test
    void of_createsAWaitingRequestWithoutTag() {
        // given
        CustomerId customerId = CustomerId.random();

        // when
        RfidTagAssignment assignment = RfidTagAssignment.of(customerId, RfidTagAssignmentType.TEMPORARY);

        // then
        assertThat(assignment.getId()).isNotNull();
        assertThat(assignment.getCustomerId()).isEqualTo(customerId);
        assertThat(assignment.getAssignmentType()).isEqualTo(RfidTagAssignmentType.TEMPORARY);
        assertThat(assignment.isWaitingForAssignment()).isTrue();
        assertThat(assignment.getRfidTagId()).isNull();
        assertThat(assignment.getAssignmentDate()).isNull();
    }

    // ------------------------------------------------------------------------
    // assignRfidTag

    @Test
    void assignRfidTag_ofAWaitingOne_assignsTheTagAndReturnsTheEvent() {
        // given
        RfidTagAssignment assignment = waiting();
        RfidTagId rfidTagId = RandomValues.rfidTagId();

        // when
        List<SchaffbarEvent> events = assignment.assignRfidTag(rfidTagId);

        // then
        assertThat(assignment.isAssigned()).isTrue();
        assertThat(assignment.getRfidTagId()).isEqualTo(rfidTagId);
        assertThat(assignment.getAssignmentDate()).isNotNull();

        RfidTagAssignedPayload payload = expectSingleEvent(events, EventType.RFID_TAG_ASSIGNED, assignment) //
                .deserializeAndValidatePayload(RfidTagAssignedPayload.class);
        assertThat(payload.customerId()).isEqualTo(assignment.getCustomerId());
        assertThat(payload.rfidTagId()).isEqualTo(rfidTagId);
        assertThat(payload.assignmentDate()).isEqualTo(assignment.getAssignmentDate());
    }

    @Test
    void assignRfidTag_ofAnAssignedOne_isRejected() {
        // given
        RfidTagAssignment assignment = assigned();

        // when
        Throwable thrown = catchThrowable(() -> assignment.assignRfidTag(RandomValues.rfidTagId()));

        // then
        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }

    // ------------------------------------------------------------------------
    // unassignRfidTag

    @Test
    void unassignRfidTag_ofAnAssignedOne_returnsTheEventWithTheHistoryEntry() {
        // given
        RfidTagAssignment assignment = assigned();
        RfidTagAssignmentHistory historyEntry = RfidTagAssignmentHistory.of(assignment);

        // when
        List<SchaffbarEvent> events = assignment.unassignRfidTag(historyEntry);

        // then
        RfidTagUnassignedPayload payload = expectSingleEvent(events, EventType.RFID_TAG_UNASSIGNED, assignment) //
                .deserializeAndValidatePayload(RfidTagUnassignedPayload.class);
        assertThat(payload.customerId()).isEqualTo(assignment.getCustomerId());
        assertThat(payload.rfidTagId()).isEqualTo(assignment.getRfidTagId());
        assertThat(payload.assignmentType()).isEqualTo(assignment.getAssignmentType().name());
        assertThat(payload.assignmentDate()).isEqualTo(assignment.getAssignmentDate());
        assertThat(payload.unassignmentDate()).isEqualTo(historyEntry.getUnassignmentDate());
    }

    @Test
    void unassignRfidTag_ofAWaitingOne_isRejected() {
        // given
        RfidTagAssignment assignment = waiting();

        // when
        Throwable thrown = catchThrowable(() -> assignment.unassignRfidTag(null));

        // then
        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }

    // ------------------------------------------------------------------------
    // cancel

    @Test
    void cancel_ofAWaitingOne_returnsTheEvent() {
        // given
        RfidTagAssignment assignment = waiting();

        // when
        List<SchaffbarEvent> events = assignment.cancel();

        // then
        RfidTagAssignmentCancelledPayload payload = expectSingleEvent(events, EventType.RFID_TAG_ASSIGNMENT_CANCELLED, assignment) //
                .deserializeAndValidatePayload(RfidTagAssignmentCancelledPayload.class);
        assertThat(payload.customerId()).isEqualTo(assignment.getCustomerId());
    }

    @Test
    void cancel_ofAnAssignedOne_isRejected() {
        // given
        RfidTagAssignment assignment = assigned();

        // when
        Throwable thrown = catchThrowable(assignment::cancel);

        // then
        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }

    // ------------------------------------------------------------------------
    // given

    private RfidTagAssignment waiting() {
        return RfidTagAssignment.of(CustomerId.random(), RfidTagAssignmentType.FIXED);
    }

    private RfidTagAssignment assigned() {
        RfidTagAssignment assignment = waiting();
        assignment.assignRfidTag(RandomValues.rfidTagId());

        return assignment;
    }

    // ------------------------------------------------------------------------
    // then

    private SchaffbarEvent expectSingleEvent(List<SchaffbarEvent> events, EventType type, RfidTagAssignment assignment) {
        assertThat(events).hasSize(1);
        SchaffbarEvent event = events.getFirst();
        assertThat(event.getType()).isEqualTo(type);
        assertThat(event.getAggregateId()).isEqualTo(assignment.getId().getValue().toString());

        return event;
    }

}
