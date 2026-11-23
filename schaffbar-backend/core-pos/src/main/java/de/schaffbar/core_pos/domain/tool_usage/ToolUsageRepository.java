package de.schaffbar.core_pos.domain.tool_usage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;

@Repository
interface ToolUsageRepository extends JpaRepository<ToolUsage, UUID> {

    List<ToolUsage> findByCustomerIdAndEndTimeIsNull(UUID customerId);

    default List<ToolUsage> findByCustomerIdAndEndTimeIsNull(CustomerId customerId) {
        return findByCustomerIdAndEndTimeIsNull(customerId.getValue());
    }

    List<ToolUsage> findByCustomerIdAndToolIdAndEndTimeIsNull(UUID customerId, UUID toolId);

    default Optional<ToolUsage> findByCustomerIdAndToolIdAndEndTimeIsNull(CustomerId customerId, ToolId toolId) {
        List<ToolUsage> usages = findByCustomerIdAndToolIdAndEndTimeIsNull(customerId.getValue(), toolId.getValue());
        if (usages.size() > 1) {
            throw new IllegalStateException("More than one running tool usage of customer [id: " + customerId.getValue() + "] on tool [id: "
                    + toolId.getValue() + "]");
        }

        return usages.stream().findFirst();
    }

    List<ToolUsage> findByToolIdAndEndTimeIsNull(UUID toolId);

    default Optional<ToolUsage> findByToolIdAndEndTimeIsNull(ToolId toolId) {
        List<ToolUsage> usages = findByToolIdAndEndTimeIsNull(toolId.getValue());
        if (usages.size() > 1) {
            throw new IllegalStateException("More than one running tool usage on tool [id: " + toolId.getValue() + "]");
        }

        return usages.stream().findFirst();
    }

    long countByCustomerIdAndEndTimeIsNull(UUID customerId);

    default long countByCustomerIdAndEndTimeIsNull(CustomerId customerId) {
        return countByCustomerIdAndEndTimeIsNull(customerId.getValue());
    }

    List<ToolUsage> findByWorkshopSessionId(UUID workshopSessionId);

    default List<ToolUsage> findByWorkshopSessionId(WorkshopSessionId workshopSessionId) {
        return findByWorkshopSessionId(workshopSessionId.getValue());
    }

    boolean existsByWorkshopSessionIdAndEndTimeIsNull(UUID workshopSessionId);

    default boolean existsByWorkshopSessionIdAndEndTimeIsNull(WorkshopSessionId workshopSessionId) {
        return existsByWorkshopSessionIdAndEndTimeIsNull(workshopSessionId.getValue());
    }

    List<ToolUsage> findByEndTimeIsNull();

}
