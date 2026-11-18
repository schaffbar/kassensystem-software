package de.schaffbar.core_pos.domain.tool_certification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;

@Repository
interface ToolCertificationRepository extends JpaRepository<ToolCertification, UUID> {

    Optional<ToolCertification> findByCustomerIdAndToolId(UUID customerId, UUID toolId);

    default Optional<ToolCertification> findByCustomerIdAndToolId(CustomerId customerId, ToolId toolId) {
        return findByCustomerIdAndToolId(customerId.getValue(), toolId.getValue());
    }

    List<ToolCertification> findByCustomerId(UUID customerId);

    default List<ToolCertification> findByCustomerId(CustomerId customerId) {
        return findByCustomerId(customerId.getValue());
    }

    List<ToolCertification> findByToolId(UUID toolId);

    default List<ToolCertification> findByToolId(ToolId toolId) {
        return findByToolId(toolId.getValue());
    }

}
