package de.schaffbar.core_pos.domain.rfid_tag_assignment;

import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentViews.RfidTagAssignmentHistoryView;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentViews.RfidTagAssignmentView;
import de.schaffbar.core_pos.shared.cqrs.Command;
import de.schaffbar.core_pos.shared.cqrs.Query;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.event.outbox.OutboxEventStore;
import de.schaffbar.core_pos.shared.exception.CustomerHasRfidTagAssignmentException;
import de.schaffbar.core_pos.shared.exception.NoRfidTagAssignedException;
import de.schaffbar.core_pos.shared.exception.NoRfidTagAssignmentWaitingException;
import de.schaffbar.core_pos.shared.exception.RfidTagAlreadyAssignedException;
import de.schaffbar.core_pos.shared.exception.RfidTagAssignmentAlreadyWaitingException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.RfidTagId;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class RfidTagAssignmentService {

    private final @NonNull RfidTagAssignmentRepository rfidTagAssignmentRepository;

    private final @NonNull RfidTagAssignmentHistoryRepository rfidTagAssignmentHistoryRepository;

    private final @NonNull OutboxEventStore outboxEventStore;

    // ------------------------------------------------------------------------
    // query

    @Query
    public List<RfidTagAssignmentView> getRfidTagAssignments() {
        return this.rfidTagAssignmentRepository.findAll().stream() //
                .map(RfidTagAssignmentViews.MAPPER::toRfidTagAssignmentView) //
                .toList();
    }

    @Query
    public Optional<RfidTagAssignmentView> getRfidTagAssignment(@NotNull @Valid CustomerId customerId) {
        return this.rfidTagAssignmentRepository.findByCustomerId(customerId) //
                .map(RfidTagAssignmentViews.MAPPER::toRfidTagAssignmentView);
    }

    @Query
    public Optional<RfidTagAssignmentView> getRfidTagAssignment(@NotNull @Valid RfidTagId rfidTagId) {
        return this.rfidTagAssignmentRepository.findByRfidTagId(rfidTagId) //
                .map(RfidTagAssignmentViews.MAPPER::toRfidTagAssignmentView);
    }

    @Query
    public List<RfidTagAssignmentHistoryView> getRfidTagAssignmentHistory() {
        return this.rfidTagAssignmentHistoryRepository.findAll().stream() //
                .map(RfidTagAssignmentViews.MAPPER::toRfidTagAssignmentHistoryView) //
                .toList();
    }

    // ------------------------------------------------------------------------
    // command

    @Command
    public void requestRfidTagAssignment(@NotNull @Valid CustomerId customerId, @NotNull RfidTagAssignmentType assignmentType) {
        ensureCustomerHasNoRfidTagAssignment(customerId);
        ensureNoRfidTagAssignmentWaiting(customerId);

        RfidTagAssignment assignment = RfidTagAssignment.of(customerId, assignmentType);
        RfidTagAssignment savedAssignment = this.rfidTagAssignmentRepository.save(assignment);
        List<SchaffbarEvent> events = List.of(RfidTagAssignmentEventFactory.rfidTagAssignmentRequested(savedAssignment));

        this.outboxEventStore.save(events);

        log.info("Requested RFID tag assignment for customer {} and published events {}", customerId, events);
    }

    @Command
    public void cancelRfidTagAssignmentRequest(@NotNull @Valid CustomerId customerId) {
        RfidTagAssignment assignment = this.rfidTagAssignmentRepository.findByCustomerId(customerId) //
                .filter(RfidTagAssignment::isWaitingForAssignment) //
                .orElseThrow(() -> NoRfidTagAssignmentWaitingException.forCustomer(customerId));

        List<SchaffbarEvent> events = assignment.cancel();
        this.rfidTagAssignmentRepository.delete(assignment);

        this.outboxEventStore.save(events);

        log.info("Cancelled RFID tag assignment request of customer {} and published events {}", customerId, events);
    }

    @Command
    public void assignRfidTag(@NotNull @Valid RfidTagId rfidTagId) {
        ensureRfidTagUnassigned(rfidTagId);

        RfidTagAssignment assignment = this.rfidTagAssignmentRepository.findWaitingForAssignment() //
                .orElseThrow(() -> NoRfidTagAssignmentWaitingException.forRfidTag(rfidTagId));

        List<SchaffbarEvent> events = assignment.assignRfidTag(rfidTagId);

        this.outboxEventStore.save(events);

        log.info("Assigned RFID tag {} and published events {}", rfidTagId, events);
    }

    @Command
    public void unassignRfidTag(@NotNull @Valid CustomerId customerId) {
        RfidTagAssignment assignment = this.rfidTagAssignmentRepository.findByCustomerId(customerId) //
                .filter(RfidTagAssignment::isAssigned) //
                .orElseThrow(() -> new NoRfidTagAssignedException(customerId));

        RfidTagAssignmentHistory historyEntry = RfidTagAssignmentHistory.of(assignment);
        List<SchaffbarEvent> events = assignment.unassignRfidTag(historyEntry);
        this.rfidTagAssignmentHistoryRepository.save(historyEntry);
        this.rfidTagAssignmentRepository.delete(assignment);

        this.outboxEventStore.save(events);

        log.info("Unassigned RFID tag from customer {} and published events {}", customerId, events);
    }

    // ------------------------------------------------------------------------
    // helper

    // TA-1
    private void ensureCustomerHasNoRfidTagAssignment(CustomerId customerId) {
        this.rfidTagAssignmentRepository.findByCustomerId(customerId) //
                .ifPresent(this::throwCustomerHasRfidTagAssignmentException);
    }

    // TA-3
    private void ensureNoRfidTagAssignmentWaiting(CustomerId customerId) {
        this.rfidTagAssignmentRepository.findWaitingForAssignment() //
                .ifPresent(waiting -> throwRfidTagAssignmentAlreadyWaitingException(customerId, waiting));
    }

    // TA-2
    private void ensureRfidTagUnassigned(RfidTagId rfidTagId) {
        this.rfidTagAssignmentRepository.findByRfidTagId(rfidTagId) //
                .ifPresent(existing -> throwRfidTagAlreadyAssignedException(rfidTagId, existing));
    }

    private void throwCustomerHasRfidTagAssignmentException(RfidTagAssignment existing) {
        throw new CustomerHasRfidTagAssignmentException(existing.getCustomerId());
    }

    private void throwRfidTagAssignmentAlreadyWaitingException(CustomerId customerId, RfidTagAssignment waiting) {
        throw new RfidTagAssignmentAlreadyWaitingException(customerId, waiting.getCustomerId());
    }

    private void throwRfidTagAlreadyAssignedException(RfidTagId rfidTagId, RfidTagAssignment existing) {
        throw new RfidTagAlreadyAssignedException(rfidTagId, existing.getCustomerId());
    }

}
