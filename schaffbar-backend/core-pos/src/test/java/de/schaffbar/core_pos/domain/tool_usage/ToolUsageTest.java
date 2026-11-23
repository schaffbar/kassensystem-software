package de.schaffbar.core_pos.domain.tool_usage;

import static de.schaffbar.core_pos.builder.CommandBuilder.startToolUsageCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.schaffbar.core_pos.domain.tool_usage.ToolUsageCommands.StartToolUsageCommand;
import de.schaffbar.core_pos.shared.billing.BillableDuration;
import de.schaffbar.core_pos.shared.event.EventType;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.event.payload.ToolUsageEventPayload.ToolUsageStoppedPayload;

class ToolUsageTest {

    // ------------------------------------------------------------------------
    // of

    @Test
    void of_startsARunningUsage() {
        // given
        StartToolUsageCommand command = startToolUsageCommand().build();

        // when
        ToolUsage usage = ToolUsage.of(command);

        // then
        assertThat(usage.getId()).isNotNull();
        assertThat(usage.getCustomerId()).isEqualTo(command.customerId());
        assertThat(usage.getToolId()).isEqualTo(command.toolId());
        assertThat(usage.getWorkshopSessionId()).isEqualTo(command.workshopSessionId());
        assertThat(usage.getStartTime()).isNotNull();
        assertThat(usage.getEndTime()).isNull();
    }

    // ------------------------------------------------------------------------
    // stop

    @Test
    void stop_endsTheUsageAndReturnsTheEvent() {
        // given
        ToolUsage usage = givenRunningUsage();

        // when
        List<SchaffbarEvent> events = usage.stop();

        // then
        assertThat(usage.getEndTime()).isAfterOrEqualTo(usage.getStartTime());

        ToolUsageStoppedPayload payload = expectSingleEvent(events, EventType.TOOL_USAGE_STOPPED, usage) //
                .deserializeAndValidatePayload(ToolUsageStoppedPayload.class);
        assertThat(payload.id()).isEqualTo(usage.getId());
        assertThat(payload.customerId()).isEqualTo(usage.getCustomerId());
        assertThat(payload.toolId()).isEqualTo(usage.getToolId());
        assertThat(payload.workshopSessionId()).isEqualTo(usage.getWorkshopSessionId());
        assertThat(payload.startTime()).isEqualTo(usage.getStartTime());
        assertThat(payload.endTime()).isEqualTo(usage.getEndTime());
    }

    @Test
    void stop_rejectsAStoppedUsage() {
        // given
        ToolUsage usage = givenStoppedUsage();
        Instant endTime = usage.getEndTime();

        // when
        Throwable thrown = catchThrowable(usage::stop);

        // then
        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        assertThat(usage.getEndTime()).isEqualTo(endTime);
    }

    // ------------------------------------------------------------------------
    // getBillableDuration

    @Test
    void getBillableDuration_isNullWhileRunning() {
        // given
        ToolUsage usage = givenRunningUsage();

        // when
        BillableDuration duration = usage.getBillableDuration();

        // then
        assertThat(duration).isNull();
    }

    @Test
    void getBillableDuration_ofAStoppedUsage_isFromStartToEnd() {
        // given
        ToolUsage usage = givenStoppedUsage();

        // when
        BillableDuration duration = usage.getBillableDuration();

        // then
        assertThat(duration).isEqualTo(BillableDuration.between(usage.getStartTime(), usage.getEndTime()));
    }

    // ------------------------------------------------------------------------
    // getBillableDurationUntil

    @Test
    void getBillableDurationUntil_countsARunningUsageUntilNow() {
        // given
        ToolUsage usage = givenRunningUsage();
        Instant now = usage.getStartTime() //
                .plus(Duration.ofMinutes(6).plusSeconds(1));

        // when
        BillableDuration duration = usage.getBillableDurationUntil(now);

        // then
        assertThat(duration.getMinutes()).isEqualTo(7);
        assertThat(duration.getUnits()).isEqualTo(2);
    }

    @Test
    void getBillableDurationUntil_ignoresNowForAFinishedUsage() {
        // given
        ToolUsage usage = givenStoppedUsage();

        // when
        BillableDuration duration = usage.getBillableDurationUntil(Instant.now().plus(Duration.ofHours(1)));

        // then
        assertThat(duration).isEqualTo(usage.getBillableDuration());
    }

    // ------------------------------------------------------------------------
    // given

    private ToolUsage givenRunningUsage() {
        return ToolUsage.of(startToolUsageCommand().build());
    }

    private ToolUsage givenStoppedUsage() {
        ToolUsage usage = givenRunningUsage();
        usage.stop();

        return usage;
    }

    // ------------------------------------------------------------------------
    // then

    private SchaffbarEvent expectSingleEvent(List<SchaffbarEvent> events, EventType type, ToolUsage usage) {
        assertThat(events).hasSize(1);
        SchaffbarEvent event = events.getFirst();
        assertThat(event.getType()).isEqualTo(type);
        assertThat(event.getAggregateId()).isEqualTo(usage.getId().getValue().toString());

        return event;
    }

}
