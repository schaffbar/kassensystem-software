package de.schaffbar.core_pos.domain.workshop_usage;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import de.schaffbar.core_pos.shared.billing.BillableDuration;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;

class WorkshopUsageTest {

    @Test
    void hasNoFinalBillableDurationWhileRunning() {
        // given
        WorkshopUsage usage = workshopUsage();

        // when / then
        assertThat(usage.getBillableDuration()).isNull();
    }

    @Test
    void countsARunningUsageUntilNow() {
        // given
        WorkshopUsage usage = workshopUsage();
        Instant now = usage.getEntryTime() //
                .plus(Duration.ofMinutes(13).plusSeconds(1));

        // when
        BillableDuration duration = usage.getBillableDurationUntil(now);

        // then
        assertThat(duration.getMinutes()).isEqualTo(14);
        assertThat(duration.getUnits()).isEqualTo(3);
    }

    @Test
    void ignoresNowForAFinishedUsage() {
        // given
        WorkshopUsage usage = workshopUsage();
        usage.exit();

        // when
        BillableDuration duration = usage.getBillableDurationUntil(Instant.now().plus(Duration.ofHours(1)));

        // then
        assertThat(duration).isEqualTo(usage.getBillableDuration());
        assertThat(duration.getDuration()).isEqualTo(usage.getDuration());
    }

    // ------------------------------------------------------------------------
    // helper

    private WorkshopUsage workshopUsage() {
        return WorkshopUsage.of(CustomerId.random(), WorkshopSessionId.random());
    }

}
