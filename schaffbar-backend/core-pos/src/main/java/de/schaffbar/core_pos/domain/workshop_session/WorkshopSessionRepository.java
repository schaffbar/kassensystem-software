package de.schaffbar.core_pos.domain.workshop_session;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.schaffbar.core_pos.shared.id.CustomerId;

@Repository
interface WorkshopSessionRepository extends JpaRepository<WorkshopSession, UUID> {

    List<WorkshopSession> findByCustomerIdAndStatus(UUID customerId, WorkshopSessionStatus status);

    default List<WorkshopSession> findByCustomerIdAndStatus(CustomerId customerId, WorkshopSessionStatus status) {
        return findByCustomerIdAndStatus(customerId.getValue(), status);
    }

    default Optional<WorkshopSession> findOpenWorkshopSession(CustomerId customerId) {
        List<WorkshopSession> workshopSessions = findByCustomerIdAndStatus(customerId, WorkshopSessionStatus.OPEN);
        if (workshopSessions.size() > 1) {
            throw new IllegalStateException("More than one open workshop session of customer [id: " + customerId.getValue() + "]");
        }

        return workshopSessions.stream().findFirst();
    }

}
