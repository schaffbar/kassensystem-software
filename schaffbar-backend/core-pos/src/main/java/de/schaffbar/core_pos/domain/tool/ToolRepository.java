package de.schaffbar.core_pos.domain.tool;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.IpAddress;
import de.schaffbar.core_pos.shared.id.RfidReaderId;

@Repository
interface ToolRepository extends JpaRepository<Tool, UUID> {

    List<Tool> findByNameIgnoreCase(String name);

    Optional<Tool> findByIpAddress(String ipAddress);

    default Optional<Tool> findByIpAddress(IpAddress ipAddress) {
        return findByIpAddress(ipAddress.getValue());
    }

    List<Tool> findByRfidReaderId(UUID rfidReaderId);

    default Optional<Tool> findByRfidReaderId(RfidReaderId rfidReaderId) {
        List<Tool> tools = findByRfidReaderId(rfidReaderId.getValue());
        if (tools.size() > 1) {
            throw new IllegalStateException("More than one tool with RFID reader [id: " + rfidReaderId.getValue() + "]");
        }

        return tools.stream().findFirst();
    }

    List<Tool> findByInstructorIdsContaining(UUID instructorId);

    default List<Tool> findByInstructorIdsContaining(CustomerId instructorId) {
        return findByInstructorIdsContaining(instructorId.getValue());
    }

}
