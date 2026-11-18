package de.schaffbar.core_pos.domain.rfid_reader;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.schaffbar.core_pos.shared.id.MacAddress;

@Repository
interface RfidReaderRepository extends JpaRepository<RfidReader, UUID> {

    Optional<RfidReader> findByMacAddress(String macAddress);

    default Optional<RfidReader> findByMacAddress(MacAddress macAddress) {
        return findByMacAddress(macAddress.getValue());
    }

}
