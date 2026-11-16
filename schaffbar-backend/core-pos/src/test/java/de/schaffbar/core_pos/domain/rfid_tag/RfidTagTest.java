package de.schaffbar.core_pos.domain.rfid_tag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.schaffbar.core_pos.builder.RandomValues;
import de.schaffbar.core_pos.shared.id.RfidTagId;

class RfidTagTest {

    @Test
    void of_createsAnActiveTagWithItsHardwareId() {
        // given
        RfidTagId rfidTagId = RandomValues.rfidTagId();

        // when
        RfidTag rfidTag = RfidTag.of(rfidTagId);

        // then
        assertThat(rfidTag.getId()).isEqualTo(rfidTagId);
        assertThat(rfidTag.isActive()).isTrue();
        assertThat(rfidTag.getCreatedAt()).isNotNull();
    }

}
