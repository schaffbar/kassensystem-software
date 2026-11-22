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
        RfidTagAssignment assignment = givenWaiting();
        RfidTagId rfidTagId = RandomValues.rfidTagId();

        // when
        List<SchaffbarEvent> events = assignment.assignRfidTag(rfidTagId);

        // then
        assertThat(assignment.isAssigned()).isTrue();
        assertThat(assignment.getRfidTagId()).isEqualTo(rfidTagId);
        assertThat(assignment.getAssignmentDate()).isNotNull();

        RfidTagAssignedPayload payload = expectSingleEvent(events, EventType.RFID_TAG_ASSIGNED, assignment) //
                .deserializeAndValidatePayload(RfidTagAssignedPayload.class);
        assertThat(payload.id()).isEqualTo(assignment.getId());
        assertThat(payload.customerId()).isEqualTo(assignment.getCustomerId());
        assertThat(payload.rfidTagId()).isEqualTo(rfidTagId);
        assertThat(payload.assignmentDate()).isEqualTo(assignment.getAssignmentDate());
    }

    @Test
    void assignRfidTag_ofAnAssignedOne_isRejected() {
        // given
        RfidTagAssignment assignment = givenAssigned();

        // when
        Throwable thrown = catchThrowable(() -> assignment.assignRfidTag(RandomValues.rfidTagId()));

        // then
        expectRejected(thrown, assignment, RfidTagAssignmentStatus.ASSIGNED);
    }

    // ------------------------------------------------------------------------
    // unassignRfidTag

    @Test
    void unassignRfidTag_ofAnAssignedOne_returnsTheEventWithTheHistoryEntry() {
        // given
        RfidTagAssignment assignment = givenAssigned();
        RfidTagAssignmentHistory historyEntry = RfidTagAssignmentHistory.of(assignment);

        // when
        List<SchaffbarEvent> events = assignment.unassignRfidTag(historyEntry);

        // then
        RfidTagUnassignedPayload payload = expectSingleEvent(events, EventType.RFID_TAG_UNASSIGNED, assignment) //
                .deserializeAndValidatePayload(RfidTagUnassignedPayload.class);
        assertThat(payload.id()).isEqualTo(assignment.getId());
        assertThat(payload.customerId()).isEqualTo(assignment.getCustomerId());
        assertThat(payload.rfidTagId()).isEqualTo(assignment.getRfidTagId());
        assertThat(payload.assignmentType()).isEqualTo(assignment.getAssignmentType());
        assertThat(payload.assignmentDate()).isEqualTo(assignment.getAssignmentDate());
        assertThat(payload.unassignmentDate()).isEqualTo(historyEntry.getUnassignmentDate());
    }

    @Test
    void unassignRfidTag_ofAWaitingOne_isRejected() {
        // given
        RfidTagAssignment assignment = givenWaiting();

        // when
        Throwable thrown = catchThrowable(() -> assignment.unassignRfidTag(null));

        // then
        expectRejected(thrown, assignment, RfidTagAssignmentStatus.WAITING_FOR_ASSIGNMENT);
    }

    // ------------------------------------------------------------------------
    // cancel

    @Test
    void cancel_ofAWaitingOne_returnsTheEvent() {
        // given
        RfidTagAssignment assignment = givenWaiting();

        // when
        List<SchaffbarEvent> events = assignment.cancel();

        // then
        RfidTagAssignmentCancelledPayload payload = expectSingleEvent(events, EventType.RFID_TAG_ASSIGNMENT_CANCELLED, assignment) //
                .deserializeAndValidatePayload(RfidTagAssignmentCancelledPayload.class);
        assertThat(payload.id()).isEqualTo(assignment.getId());
        assertThat(payload.customerId()).isEqualTo(assignment.getCustomerId());
    }

    @Test
    void cancel_ofAnAssignedOne_isRejected() {
        // given
        RfidTagAssignment assignment = givenAssigned();

        // when
        Throwable thrown = catchThrowable(assignment::cancel);

        // then
        expectRejected(thrown, assignment, RfidTagAssignmentStatus.ASSIGNED);
    }

    // ------------------------------------------------------------------------
    // given

    private RfidTagAssignment givenWaiting() {
        return RfidTagAssignment.of(CustomerId.random(), RfidTagAssignmentType.FIXED);
    }

    private RfidTagAssignment givenAssigned() {
        RfidTagAssignment assignment = givenWaiting();
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

    // a guard against a bug – the service finds the assignment in the state the command needs (E29)
    private void expectRejected(Throwable thrown, RfidTagAssignment assignment, RfidTagAssignmentStatus unchangedStatus) {
        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        assertThat(assignment.getStatus()).isEqualTo(unchangedStatus);
    }

}
