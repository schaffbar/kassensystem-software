package de.schaffbar.core_pos.domain.rfid_tag_assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.schaffbar.core_pos.builder.RandomValues;
import de.schaffbar.core_pos.shared.event.EventType;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.event.outbox.OutboxEventStore;
import de.schaffbar.core_pos.shared.event.payload.RfidTagAssignmentEventPayload.RfidTagAssignmentRequestedPayload;
import de.schaffbar.core_pos.shared.exception.CustomerHasRfidTagAssignmentException;
import de.schaffbar.core_pos.shared.exception.NoRfidTagAssignedException;
import de.schaffbar.core_pos.shared.exception.NoRfidTagAssignmentWaitingException;
import de.schaffbar.core_pos.shared.exception.RfidTagAlreadyAssignedException;
import de.schaffbar.core_pos.shared.exception.RfidTagAssignmentAlreadyWaitingException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.RfidTagId;

@ExtendWith(MockitoExtension.class)
class RfidTagAssignmentServiceTest {

    @Mock
    private RfidTagAssignmentRepository rfidTagAssignmentRepository;

    @Mock
    private RfidTagAssignmentHistoryRepository rfidTagAssignmentHistoryRepository;

    @Mock
    private OutboxEventStore outboxEventStore;

    @Captor
    private ArgumentCaptor<List<SchaffbarEvent>> events;

    @Captor
    private ArgumentCaptor<RfidTagAssignment> savedAssignment;

    @Captor
    private ArgumentCaptor<RfidTagAssignmentHistory> savedHistoryEntry;

    @InjectMocks
    private RfidTagAssignmentService rfidTagAssignmentService;

    // ------------------------------------------------------------------------
    // requestRfidTagAssignment

    @Test
    void requestRfidTagAssignment_savesAWaitingAssignmentAndItsEvent() {
        // given
        CustomerId customerId = givenCustomerWithoutAssignment();
        givenNoRequestWaiting();
        givenSaveReturnsTheAssignment();

        // when
        this.rfidTagAssignmentService.requestRfidTagAssignment(customerId, RfidTagAssignmentType.TEMPORARY);

        // then
        RfidTagAssignment assignment = expectSavedAssignment();
        assertThat(assignment.isWaitingForAssignment()).isTrue();
        assertThat(assignment.getAssignmentType()).isEqualTo(RfidTagAssignmentType.TEMPORARY);
        RfidTagAssignmentRequestedPayload payload = expectSavedEvent(EventType.RFID_TAG_ASSIGNMENT_REQUESTED, assignment) //
                .deserializeAndValidatePayload(RfidTagAssignmentRequestedPayload.class);
        assertThat(payload.id()).isEqualTo(assignment.getId());
        assertThat(payload.customerId()).isEqualTo(customerId);
        assertThat(payload.assignmentType()).isEqualTo(RfidTagAssignmentType.TEMPORARY);
    }

    @Test
    void requestRfidTagAssignment_rejectsACustomerWithAssignment() {
        // given
        RfidTagAssignment assignment = givenCustomerWithTag();

        // when
        Throwable thrown = catchThrowable(() -> this.rfidTagAssignmentService.requestRfidTagAssignment(assignment.getCustomerId(), RfidTagAssignmentType.FIXED));

        // then
        assertThat(thrown) //
                .isInstanceOf(CustomerHasRfidTagAssignmentException.class) //
                .hasFieldOrPropertyWithValue("code", CustomerHasRfidTagAssignmentException.CODE);
        expectNothingSaved();
    }

    @Test
    void requestRfidTagAssignment_rejectsACustomerWhoseRequestIsStillWaiting() {
        // given
        RfidTagAssignment assignment = givenWaitingRequestOfTheCustomer();

        // when
        Throwable thrown = catchThrowable(() -> this.rfidTagAssignmentService.requestRfidTagAssignment(assignment.getCustomerId(), RfidTagAssignmentType.FIXED));

        // then
        assertThat(thrown) //
                .isInstanceOf(CustomerHasRfidTagAssignmentException.class) //
                .hasFieldOrPropertyWithValue("code", CustomerHasRfidTagAssignmentException.CODE);
        expectNothingSaved();
    }

    @Test
    void requestRfidTagAssignment_rejectsWhileAnotherRequestIsWaiting() {
        // given
        CustomerId customerId = givenCustomerWithoutAssignment();
        givenWaitingRequest();

        // when
        Throwable thrown = catchThrowable(() -> this.rfidTagAssignmentService.requestRfidTagAssignment(customerId, RfidTagAssignmentType.FIXED));

        // then
        assertThat(thrown) //
                .isInstanceOf(RfidTagAssignmentAlreadyWaitingException.class) //
                .hasFieldOrPropertyWithValue("code", RfidTagAssignmentAlreadyWaitingException.CODE);
        expectNothingSaved();
    }

    // ------------------------------------------------------------------------
    // assignRfidTag

    @Test
    void assignRfidTag_assignsTheTagToTheWaitingRequestAndSavesTheEvent() {
        // given
        RfidTagId rfidTagId = givenUnassignedTag();
        RfidTagAssignment assignment = givenWaitingRequest();

        // when
        this.rfidTagAssignmentService.assignRfidTag(rfidTagId);

        // then
        assertThat(assignment.isAssigned()).isTrue();
        assertThat(assignment.getRfidTagId()).isEqualTo(rfidTagId);
        assertThat(assignment.getAssignmentDate()).isNotNull();
        expectSavedEvent(EventType.RFID_TAG_ASSIGNED, assignment);
    }

    @Test
    void assignRfidTag_rejectsAnAssignedTag() {
        // given
        RfidTagAssignment assignment = givenAssignedTag();

        // when
        Throwable thrown = catchThrowable(() -> this.rfidTagAssignmentService.assignRfidTag(assignment.getRfidTagId()));

        // then
        assertThat(thrown) //
                .isInstanceOf(RfidTagAlreadyAssignedException.class) //
                .hasFieldOrPropertyWithValue("code", RfidTagAlreadyAssignedException.CODE);
        expectNoEvent();
    }

    @Test
    void assignRfidTag_rejectsWithoutWaitingRequest() {
        // given
        RfidTagId rfidTagId = givenUnassignedTag();
        givenNoRequestWaiting();

        // when
        Throwable thrown = catchThrowable(() -> this.rfidTagAssignmentService.assignRfidTag(rfidTagId));

        // then
        assertThat(thrown) //
                .isInstanceOf(NoRfidTagAssignmentWaitingException.class) //
                .hasFieldOrPropertyWithValue("code", NoRfidTagAssignmentWaitingException.CODE);
        expectNoEvent();
    }

    // ------------------------------------------------------------------------
    // unassignRfidTag

    @Test
    void unassignRfidTag_writesTheHistoryDeletesTheAssignmentAndSavesTheEvent() {
        // given
        RfidTagAssignment assignment = givenCustomerWithTag();

        // when
        this.rfidTagAssignmentService.unassignRfidTag(assignment.getCustomerId());

        // then
        expectHistoryEntryOf(assignment);
        expectDeleted(assignment);
        expectSavedEvent(EventType.RFID_TAG_UNASSIGNED, assignment);
    }

    @Test
    void unassignRfidTag_rejectsACustomerWhoseRequestIsStillWaiting() {
        // given
        RfidTagAssignment assignment = givenWaitingRequestOfTheCustomer();

        // when
        Throwable thrown = catchThrowable(() -> this.rfidTagAssignmentService.unassignRfidTag(assignment.getCustomerId()));

        // then
        assertThat(thrown) //
                .isInstanceOf(NoRfidTagAssignedException.class) //
                .hasFieldOrPropertyWithValue("code", NoRfidTagAssignedException.CODE);
        expectNoHistoryEntry();
        expectNothingDeleted();
    }

    @Test
    void unassignRfidTag_rejectsACustomerWithoutAssignment() {
        // given
        CustomerId customerId = givenCustomerWithoutAssignment();

        // when
        Throwable thrown = catchThrowable(() -> this.rfidTagAssignmentService.unassignRfidTag(customerId));

        // then
        assertThat(thrown) //
                .isInstanceOf(NoRfidTagAssignedException.class) //
                .hasFieldOrPropertyWithValue("code", NoRfidTagAssignedException.CODE);
        expectNoHistoryEntry();
        expectNothingDeleted();
    }

    // ------------------------------------------------------------------------
    // cancelRfidTagAssignment

    @Test
    void cancelRfidTagAssignment_deletesTheWaitingRequestWithoutHistory() {
        // given
        RfidTagAssignment assignment = givenWaitingRequestOfTheCustomer();

        // when
        this.rfidTagAssignmentService.cancelRfidTagAssignment(assignment.getCustomerId());

        // then
        expectDeleted(assignment);
        expectNoHistoryEntry();
        expectSavedEvent(EventType.RFID_TAG_ASSIGNMENT_CANCELLED, assignment);
    }

    @Test
    void cancelRfidTagAssignment_rejectsAnAssignedTag() {
        // given
        RfidTagAssignment assignment = givenCustomerWithTag();

        // when
        Throwable thrown = catchThrowable(() -> this.rfidTagAssignmentService.cancelRfidTagAssignment(assignment.getCustomerId()));

        // then
        assertThat(thrown) //
                .isInstanceOf(NoRfidTagAssignmentWaitingException.class) //
                .hasFieldOrPropertyWithValue("code", NoRfidTagAssignmentWaitingException.CODE);
        expectNothingDeleted();
    }

    @Test
    void cancelRfidTagAssignment_rejectsACustomerWithoutAssignment() {
        // given
        CustomerId customerId = givenCustomerWithoutAssignment();

        // when
        Throwable thrown = catchThrowable(() -> this.rfidTagAssignmentService.cancelRfidTagAssignment(customerId));

        // then
        assertThat(thrown) //
                .isInstanceOf(NoRfidTagAssignmentWaitingException.class) //
                .hasFieldOrPropertyWithValue("code", NoRfidTagAssignmentWaitingException.CODE);
        expectNothingDeleted();
    }

    // ------------------------------------------------------------------------
    // given

    private CustomerId givenCustomerWithoutAssignment() {
        CustomerId customerId = CustomerId.random();
        when(this.rfidTagAssignmentRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());

        return customerId;
    }

    private RfidTagId givenUnassignedTag() {
        RfidTagId rfidTagId = RandomValues.rfidTagId();
        when(this.rfidTagAssignmentRepository.findByRfidTagId(rfidTagId)).thenReturn(Optional.empty());

        return rfidTagId;
    }

    private RfidTagAssignment givenWaitingRequest() {
        RfidTagAssignment assignment = RfidTagAssignment.of(CustomerId.random(), RfidTagAssignmentType.FIXED);
        when(this.rfidTagAssignmentRepository.findWaitingForAssignment()).thenReturn(Optional.of(assignment));

        return assignment;
    }

    private RfidTagAssignment givenWaitingRequestOfTheCustomer() {
        RfidTagAssignment assignment = RfidTagAssignment.of(CustomerId.random(), RfidTagAssignmentType.FIXED);
        when(this.rfidTagAssignmentRepository.findByCustomerId(assignment.getCustomerId())).thenReturn(Optional.of(assignment));

        return assignment;
    }

    private void givenNoRequestWaiting() {
        when(this.rfidTagAssignmentRepository.findWaitingForAssignment()).thenReturn(Optional.empty());
    }

    private RfidTagAssignment givenCustomerWithTag() {
        RfidTagAssignment assignment = assigned();
        when(this.rfidTagAssignmentRepository.findByCustomerId(assignment.getCustomerId())).thenReturn(Optional.of(assignment));

        return assignment;
    }

    private RfidTagAssignment givenAssignedTag() {
        RfidTagAssignment assignment = assigned();
        when(this.rfidTagAssignmentRepository.findByRfidTagId(assignment.getRfidTagId())).thenReturn(Optional.of(assignment));

        return assignment;
    }

    private void givenSaveReturnsTheAssignment() {
        when(this.rfidTagAssignmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ------------------------------------------------------------------------
    // then

    private RfidTagAssignment expectSavedAssignment() {
        verify(this.rfidTagAssignmentRepository).save(this.savedAssignment.capture());

        return this.savedAssignment.getValue();
    }

    private SchaffbarEvent expectSavedEvent(EventType type, RfidTagAssignment assignment) {
        verify(this.outboxEventStore).save(this.events.capture());
        assertThat(this.events.getValue()) //
                .singleElement() //
                .satisfies(event -> {
                    assertThat(event.getType()).isEqualTo(type);
                    assertThat(event.getAggregateId()).isEqualTo(assignment.getId().getValue().toString());
                });

        return this.events.getValue().getFirst();
    }

    private void expectDeleted(RfidTagAssignment assignment) {
        verify(this.rfidTagAssignmentRepository).delete(assignment);
    }

    private void expectNoHistoryEntry() {
        verifyNoInteractions(this.rfidTagAssignmentHistoryRepository);
    }

    private void expectHistoryEntryOf(RfidTagAssignment assignment) {
        verify(this.rfidTagAssignmentHistoryRepository).save(this.savedHistoryEntry.capture());
        assertThat(this.savedHistoryEntry.getValue().getRfidTagId()).isEqualTo(assignment.getRfidTagId());
    }

    private void expectNothingSaved() {
        verify(this.rfidTagAssignmentRepository, never()).save(any());
        expectNoEvent();
    }

    private void expectNothingDeleted() {
        verify(this.rfidTagAssignmentRepository, never()).delete(any());
        expectNoEvent();
    }

    private void expectNoEvent() {
        verifyNoInteractions(this.outboxEventStore);
    }

    // ------------------------------------------------------------------------
    // helper

    private static RfidTagAssignment assigned() {
        RfidTagAssignment assignment = RfidTagAssignment.of(CustomerId.random(), RfidTagAssignmentType.FIXED);
        assignment.assignRfidTag(RandomValues.rfidTagId());

        return assignment;
    }

}
