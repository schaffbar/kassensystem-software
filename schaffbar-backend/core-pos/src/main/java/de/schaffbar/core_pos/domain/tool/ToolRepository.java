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

    List<Tool> findByIpAddress(String ipAddress);

    // T-4 – checked by the service, the column has no unique constraint
    default Optional<Tool> findByIpAddress(IpAddress ipAddress) {
        List<Tool> tools = findByIpAddress(ipAddress.getValue());
        if (tools.size() > 1) {
            throw new IllegalStateException("More than one tool with IP address [" + ipAddress.getValue() + "]");
        }

        return tools.stream().findFirst();
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
