package de.schaffbar.core_pos.domain.rfid_tag_assignment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import de.schaffbar.core_pos.builder.RandomValues;
import de.schaffbar.core_pos.shared.id.CustomerId;

class RfidTagAssignmentHistoryTest {

    @Test
    void of_copiesTheAssignmentAndSetsTheUnassignmentDate() {
        // given
        RfidTagAssignment assignment = RfidTagAssignment.of(CustomerId.random(), RfidTagAssignmentType.TEMPORARY);
        assignment.assignRfidTag(RandomValues.rfidTagId());
        Instant before = Instant.now();

        // when
        RfidTagAssignmentHistory historyEntry = RfidTagAssignmentHistory.of(assignment);

        // then
        assertThat(historyEntry.getCustomerId()).isEqualTo(assignment.getCustomerId());
        assertThat(historyEntry.getRfidTagId()).isEqualTo(assignment.getRfidTagId());
        assertThat(historyEntry.getAssignmentType()).isEqualTo(RfidTagAssignmentType.TEMPORARY);
        assertThat(historyEntry.getAssignmentDate()).isEqualTo(assignment.getAssignmentDate());
        assertThat(historyEntry.getUnassignmentDate()).isAfterOrEqualTo(before);
    }

}
