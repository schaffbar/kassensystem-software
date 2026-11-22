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

    // customer_id and rfid_tag_id are unique columns (V1)
    Optional<RfidTagAssignment> findByCustomerId(UUID customerId);

    default Optional<RfidTagAssignment> findByCustomerId(CustomerId customerId) {
        return findByCustomerId(customerId.getValue());
    }

    Optional<RfidTagAssignment> findByRfidTagId(String rfidTagId);

    default Optional<RfidTagAssignment> findByRfidTagId(RfidTagId rfidTagId) {
        return findByRfidTagId(rfidTagId.getValue());
    }

    List<RfidTagAssignment> findByStatus(RfidTagAssignmentStatus status);

    // TA-3 – checked by the service, not by the database
    default Optional<RfidTagAssignment> findWaitingForAssignment() {
        List<RfidTagAssignment> assignments = findByStatus(RfidTagAssignmentStatus.WAITING_FOR_ASSIGNMENT);
        if (assignments.size() > 1) {
            throw new IllegalStateException("More than one RFID tag assignment waits for a tag: " + assignments.stream() //
                    .map(assignment -> assignment.getId().getValue().toString()) //
                    .toList());
        }

        return assignments.stream().findFirst();
    }

}
