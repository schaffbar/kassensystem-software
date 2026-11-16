package de.schaffbar.core_pos.domain.rfid_tag_assignment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.RfidTagId;

@Repository
interface RfidTagAssignmentRepository extends JpaRepository<RfidTagAssignment, UUID> {

    List<RfidTagAssignment> findByCustomerId(UUID customerId);

    default Optional<RfidTagAssignment> findByCustomerId(CustomerId customerId) {
        List<RfidTagAssignment> assignments = findByCustomerId(customerId.getValue());
        if (assignments.isEmpty()) {
            return Optional.empty();
        }

        if (assignments.size() > 1) {
            throw new IllegalStateException("More than one RFID tag assignment found");
        }

        return Optional.of(assignments.getFirst());
    }

    List<RfidTagAssignment> findByRfidTagId(String rfidTagId);

    default Optional<RfidTagAssignment> findByRfidTagId(RfidTagId rfidTagId) {
        List<RfidTagAssignment> assignments = findByRfidTagId(rfidTagId.getValue());
        if (assignments.isEmpty()) {
            return Optional.empty();
        }

        if (assignments.size() > 1) {
            throw new IllegalStateException("More than one RFID tag assignment found");
        }

        return Optional.of(assignments.getFirst());
    }

    List<RfidTagAssignment> findByStatus(RfidTagAssignmentStatus status);

    default Optional<RfidTagAssignment> findWaitingForAssignment() {
        List<RfidTagAssignment> assignments = findByStatus(RfidTagAssignmentStatus.WAITING_FOR_ASSIGNMENT);
        if (assignments.isEmpty()) {
            return Optional.empty();
        }

        if (assignments.size() > 1) {
            throw new IllegalStateException("More than one RFID tag assignment found");
        }

        return Optional.of(assignments.getFirst());
    }

}
