package de.schaffbar.core_pos.domain.rfid_tag;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.schaffbar.core_pos.shared.id.RfidTagId;

@Repository
interface RfidTagRepository extends JpaRepository<RfidTag, String> {

    default boolean existsById(RfidTagId id) {
        return existsById(id.getValue());
    }

}
