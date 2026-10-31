package de.schaffbar.core_pos.domain.tool_usage;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import de.schaffbar.core_pos.domain.tool_usage.ToolUsageCommands.StartToolUsageCommand;
import de.schaffbar.core_pos.shared.billing.BillableDuration;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;

class ToolUsage_GetBillableDurationTest {

    @Test
    void hasNoFinalBillableDurationWhileRunning() {
        // given
        ToolUsage usage = toolUsage();

        // when / then
        assertThat(usage.getBillableDuration()).isNull();
    }

    @Test
    void countsARunningUsageUntilNow() {
        // given
        ToolUsage usage = toolUsage();
        Instant now = usage.getStartTime() //
                .plus(Duration.ofMinutes(6).plusSeconds(1));

        // when
        BillableDuration duration = usage.getBillableDurationUntil(now);

        // then
        assertThat(duration.getMinutes()).isEqualTo(7);
        assertThat(duration.getUnits()).isEqualTo(2);
    }

    @Test
    void ignoresNowForAFinishedUsage() {
        ToolUsage usage = toolUsage();
        usage.stop();

        BillableDuration duration = usage.getBillableDurationUntil(Instant.now().plus(Duration.ofHours(1)));

        assertThat(duration).isEqualTo(usage.getBillableDuration());
        assertThat(duration.getDuration()).isEqualTo(usage.getDuration());
    }

    // ------------------------------------------------------------------------
    // helper

    private ToolUsage toolUsage() {
        return ToolUsage.of(new StartToolUsageCommand( //
                CustomerId.random(), //
                ToolId.random(), //
                WorkshopSessionId.random()));
    }

}
