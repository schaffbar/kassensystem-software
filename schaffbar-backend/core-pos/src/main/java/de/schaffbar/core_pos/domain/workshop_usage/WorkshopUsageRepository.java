package de.schaffbar.core_pos.domain.workshop_usage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;

@Repository
interface WorkshopUsageRepository extends JpaRepository<WorkshopUsage, UUID> {

    List<WorkshopUsage> findByCustomerIdAndExitTimeIsNull(UUID customerId);

    default Optional<WorkshopUsage> findByCustomerIdAndExitTimeIsNull(CustomerId customerId) {
        List<WorkshopUsage> workshopUsages = findByCustomerIdAndExitTimeIsNull(customerId.getValue());
        if (workshopUsages.size() > 1) {
            throw new IllegalStateException("More than one active workshop usage of customer [id: " + customerId.getValue() + "]");
        }

        return workshopUsages.stream().findFirst();
    }

    List<WorkshopUsage> findByWorkshopSessionId(UUID workshopSessionId);

    default List<WorkshopUsage> findByWorkshopSessionId(WorkshopSessionId workshopSessionId) {
        return findByWorkshopSessionId(workshopSessionId.getValue());
    }

    List<WorkshopUsage> findByExitTimeIsNull();

}
