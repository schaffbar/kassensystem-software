package de.schaffbar.core_pos.domain.workshop_usage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.schaffbar.core_pos.shared.event.EventType;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.event.payload.WorkshopUsageEventPayload.WorkshopUsageLeftPayload;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;

class WorkshopUsageTest {

    // ------------------------------------------------------------------------
    // of

    @Test
    void of_startsARunningUsage() {
        // given
        CustomerId customerId = CustomerId.random();
        WorkshopSessionId workshopSessionId = WorkshopSessionId.random();

        // when
        WorkshopUsage usage = WorkshopUsage.of(customerId, workshopSessionId);

        // then
        assertThat(usage.getId()).isNotNull();
        assertThat(usage.getCustomerId()).isEqualTo(customerId);
        assertThat(usage.getWorkshopSessionId()).isEqualTo(workshopSessionId);
        assertThat(usage.getEntryTime()).isNotNull();
        assertThat(usage.getExitTime()).isNull();
        assertThat(usage.getDuration()).isNull();
    }

    // ------------------------------------------------------------------------
    // exit

    @Test
    void exit_endsTheUsageAndReturnsTheEvent() {
        // given
        WorkshopUsage usage = givenRunningUsage();

        // when
        List<SchaffbarEvent> events = usage.exit();

        // then
        assertThat(usage.getExitTime()).isAfterOrEqualTo(usage.getEntryTime());
        assertThat(usage.getDuration()).isGreaterThanOrEqualTo(Duration.ZERO);

        assertThat(events).hasSize(1);
        SchaffbarEvent event = events.getFirst();
        assertThat(event.getType()).isEqualTo(EventType.WORKSHOP_USAGE_LEFT);
        assertThat(event.getAggregateId()).isEqualTo(usage.getId().getValue().toString());

        WorkshopUsageLeftPayload payload = event.deserializeAndValidatePayload(WorkshopUsageLeftPayload.class);
        assertThat(payload.id()).isEqualTo(usage.getId());
        assertThat(payload.customerId()).isEqualTo(usage.getCustomerId());
        assertThat(payload.workshopSessionId()).isEqualTo(usage.getWorkshopSessionId());
        assertThat(payload.entryTime()).isEqualTo(usage.getEntryTime());
        assertThat(payload.exitTime()).isEqualTo(usage.getExitTime());
    }

    @Test
    void exit_rejectsAnEndedUsage() {
        // given
        WorkshopUsage usage = givenRunningUsage();
        usage.exit();
        Instant exitTime = usage.getExitTime();

        // when
        Throwable thrown = catchThrowable(usage::exit);

        // then
        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        assertThat(usage.getExitTime()).isEqualTo(exitTime);
    }

    // ------------------------------------------------------------------------
    // given

    private WorkshopUsage givenRunningUsage() {
        return WorkshopUsage.of(CustomerId.random(), WorkshopSessionId.random());
    }

}
