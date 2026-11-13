package de.schaffbar.core_pos.domain.rfid_tag_assignment;

import java.util.List;
import java.util.Optional;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentViews.RfidTagAssignmentView;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.event.outbox.OutboxEventStore;
import de.schaffbar.core_pos.shared.exception.NoWaitingAssingmentException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.RfidTagId;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class RfidTagAssignmentService {

    private final @NonNull RfidTagAssignmentRepository rfidTagAssignmentRepository;

    private final @NonNull OutboxEventStore outboxEventStore;

    // ------------------------------------------------------------------------
    // query

    public List<RfidTagAssignmentView> getRfidTagAssignments() {
        return this.rfidTagAssignmentRepository.findAll().stream() //
                .map(RfidTagAssignmentViews.MAPPER::toRfidTagAssignmentView) //
                .toList();
    }

    public Optional<RfidTagAssignmentView> getRfidTagAssignment(@NotNull @Valid CustomerId customerId) {
        return this.rfidTagAssignmentRepository.findByCustomer(customerId) //
                .map(RfidTagAssignmentViews.MAPPER::toRfidTagAssignmentView);
    }

    public Optional<RfidTagAssignmentView> getRfidTagAssignment(@NotNull @Valid RfidTagId rfidTagId) {
        return this.rfidTagAssignmentRepository.findByRfidTag(rfidTagId) //
                .map(RfidTagAssignmentViews.MAPPER::toRfidTagAssignmentView);
    }

    // ------------------------------------------------------------------------
    // command

    @Transactional
    public void requestRfidTagAssignment(@NotNull @Valid CustomerId customerId, @NotNull RfidTagAssignmentType assignmentType) {
        if (getRfidTagAssignment(customerId).isPresent()) {
            throw new RuntimeException("RFID tag is already assigned to customer [id: " + customerId.getValue() + "]");
        }

        if (this.rfidTagAssignmentRepository.findWaitingForAssignment().isPresent()) {
            throw new RuntimeException("There is already a RFID tag assignment pending");
        }

        RfidTagAssignment assignment = RfidTagAssignment.of(customerId, assignmentType);
        RfidTagAssignment savedAssignment = this.rfidTagAssignmentRepository.save(assignment);
        List<SchaffbarEvent> events = List.of(RfidTagAssignmentEventFactory.rfidTagAssignmentRequested(savedAssignment));

        this.outboxEventStore.save(events);

        log.info("Requested RFID tag assignment for customer {} and published events {}", customerId, events);
    }

    @Transactional
    public void assignRfidTag(@NotNull @Valid RfidTagId rfidTagId) {
        if (getRfidTagAssignment(rfidTagId).isPresent()) {
            throw new RuntimeException("RFID tag [id: " + rfidTagId.getValue() + "] already assigned");
        }

        RfidTagAssignment assignment = this.rfidTagAssignmentRepository.findWaitingForAssignment() //
                .orElseThrow(() -> new NoWaitingAssingmentException(rfidTagId));

        List<SchaffbarEvent> events = assignment.assignRfidTag(rfidTagId);
        this.outboxEventStore.save(events);

        log.info("Assigned RFID tag {} and published events {}", rfidTagId, events);
    }

    @Transactional
    public void unassignRfidTag(@NotNull @Valid CustomerId customerId) {
        RfidTagAssignment assignment = this.rfidTagAssignmentRepository.findByCustomer(customerId) //
                .filter(RfidTagAssignment::isAssigned) //
                // TODO: implement custom exception
                .orElseThrow(() -> new RuntimeException("No RFID tag assigned to customer [id: " + customerId.getValue() + "]"));

        List<SchaffbarEvent> events = assignment.unassignEvents();
        this.rfidTagAssignmentRepository.delete(assignment);

        this.outboxEventStore.save(events);

        log.info("Unassigned RFID tag from customer {} and published events {}", customerId, events);
    }

    @Transactional
    public void cancelRfidTagAssignmentRequest(@NotNull @Valid CustomerId customerId) {
        RfidTagAssignment assignment = this.rfidTagAssignmentRepository.findByCustomer(customerId) //
                .filter(RfidTagAssignment::isWaitingForAssignment) //
                .orElseThrow(() -> new RuntimeException("No RFID tag assignment requested for customer [id: " + customerId.getValue() + "]"));

        List<SchaffbarEvent> events = assignment.cancelEvents();
        this.rfidTagAssignmentRepository.delete(assignment);

        this.outboxEventStore.save(events);

        log.info("Cancelled RFID tag assignment request of customer {} and published events {}", customerId, events);
    }

}
