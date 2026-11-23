package de.schaffbar.core_pos.domain.tool_usage;

import static de.schaffbar.core_pos.builder.CommandBuilder.startToolUsageCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.schaffbar.core_pos.domain.tool_usage.ToolUsageCommands.StartToolUsageCommand;
import de.schaffbar.core_pos.shared.billing.BillingTotal;
import de.schaffbar.core_pos.shared.event.EventType;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.event.outbox.OutboxEventStore;
import de.schaffbar.core_pos.shared.event.payload.ToolUsageEventPayload.ToolUsageStartedPayload;
import de.schaffbar.core_pos.shared.exception.CustomerAlreadyUsingToolException;
import de.schaffbar.core_pos.shared.exception.MaxToolUsageExceededException;
import de.schaffbar.core_pos.shared.exception.ToolAlreadyInUseByAnotherCustomerException;
import de.schaffbar.core_pos.shared.exception.ToolNotInUseByCustomerException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;

@ExtendWith(MockitoExtension.class)
class ToolUsageServiceTest {

    @Mock
    private ToolUsageRepository toolUsageRepository;

    @Mock
    private OutboxEventStore outboxEventStore;

    @Captor
    private ArgumentCaptor<List<SchaffbarEvent>> events;

    @Captor
    private ArgumentCaptor<ToolUsage> savedUsage;

    @InjectMocks
    private ToolUsageService toolUsageService;

    // ------------------------------------------------------------------------
    // getBillingTotal

    @Test
    void getBillingTotal_addsTheFinishedUsagesOfTheSession() {
        // given
        StartToolUsageCommand onFirstTool = startToolUsageCommand().build();
        ToolUsage first = ToolUsage.of(onFirstTool);
        first.stop();

        ToolUsage second = ToolUsage.of(startToolUsageCommand() //
                .customerId(onFirstTool.customerId()) //
                .workshopSessionId(onFirstTool.workshopSessionId()) //
                .build());
        second.stop();

        givenUsagesOfTheSession(onFirstTool.workshopSessionId(), first, second);

        // when
        BillingTotal total = this.toolUsageService.getBillingTotal(onFirstTool.workshopSessionId());

        // then
        assertThat(total).isEqualTo(BillingTotal.of(List.of(first.getBillableDuration(), second.getBillableDuration())));
    }

    // a guard against a bug – WorkshopSessionClose checks before that no usage is running (WS-5)
    @Test
    void getBillingTotal_rejectsASessionWithARunningUsage() {
        // given
        StartToolUsageCommand command = startToolUsageCommand().build();
        givenUsagesOfTheSession(command.workshopSessionId(), ToolUsage.of(command));

        // when
        Throwable thrown = catchThrowable(() -> this.toolUsageService.getBillingTotal(command.workshopSessionId()));

        // then
        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }

    // ------------------------------------------------------------------------
    // getBillingTotalUntil

    @Test
    void getBillingTotalUntil_countsAFinishedUsageUntilItsEndAndARunningOneUntilNow() {
        // given
        StartToolUsageCommand command = startToolUsageCommand().build();
        ToolUsage finished = ToolUsage.of(command);
        finished.stop();
        ToolUsage running = ToolUsage.of(command);
        givenUsagesOfTheSession(command.workshopSessionId(), finished, running);
        Instant now = Instant.now().plus(Duration.ofMinutes(13).plusSeconds(1));

        // when
        BillingTotal total = this.toolUsageService.getBillingTotalUntil(command.workshopSessionId(), now);

        // then
        assertThat(total).isEqualTo(BillingTotal.of(List.of(finished.getBillableDuration(), running.getBillableDurationUntil(now))));
    }

    // ------------------------------------------------------------------------
    // getBillingTotalsByToolUntil

    @Test
    void getBillingTotalsByToolUntil_addsTheUsagesOfEachToolSeparately() {
        // given – the first tool was switched off and on again, the second one runs
        StartToolUsageCommand onFirstTool = startToolUsageCommand().build();
        StartToolUsageCommand onSecondTool = startToolUsageCommand() //
                .customerId(onFirstTool.customerId()) //
                .workshopSessionId(onFirstTool.workshopSessionId()) //
                .build();
        ToolUsage finished = ToolUsage.of(onFirstTool);
        finished.stop();
        ToolUsage running = ToolUsage.of(onFirstTool);
        ToolUsage other = ToolUsage.of(onSecondTool);
        givenUsagesOfTheSession(onFirstTool.workshopSessionId(), finished, running, other);
        Instant now = Instant.now().plus(Duration.ofMinutes(13).plusSeconds(1));

        // when
        Map<ToolId, BillingTotal> totals = this.toolUsageService.getBillingTotalsByToolUntil(onFirstTool.workshopSessionId(), now);

        // then – a finished usage counts until its end, a running one until now
        assertThat(totals).containsOnly( //
                entry(onFirstTool.toolId(), BillingTotal.of(List.of(finished.getBillableDuration(), running.getBillableDurationUntil(now)))), //
                entry(onSecondTool.toolId(), BillingTotal.of(List.of(other.getBillableDurationUntil(now)))));
    }

    // ------------------------------------------------------------------------
    // startToolUsage

    @Test
    void startToolUsage_savesTheUsageAndItsStartedEvent() {
        // given
        StartToolUsageCommand command = startToolUsageCommand().build();
        givenToolNotInUse(command.toolId());
        givenCustomerNotUsingTheTool(command.customerId(), command.toolId());
        givenNumberOfRunningUsages(command.customerId(), 1);

        // when
        this.toolUsageService.startToolUsage(command);

        // then
        ToolUsage usage = expectSavedUsage();
        ToolUsageStartedPayload payload = expectSavedEvent(EventType.TOOL_USAGE_STARTED, usage) //
                .deserializeAndValidatePayload(ToolUsageStartedPayload.class);
        assertThat(payload.id()).isEqualTo(usage.getId());
        assertThat(payload.customerId()).isEqualTo(command.customerId());
        assertThat(payload.toolId()).isEqualTo(command.toolId());
        assertThat(payload.workshopSessionId()).isEqualTo(command.workshopSessionId());
        assertThat(payload.startTime()).isNotNull();
    }

    @Test
    void startToolUsage_rejectsAToolInUseByAnotherCustomer() {
        // given
        StartToolUsageCommand command = startToolUsageCommand().build();
        givenToolInUseBy(CustomerId.random(), command.toolId());

        // when
        Throwable thrown = catchThrowable(() -> this.toolUsageService.startToolUsage(command));

        // then
        assertThat(thrown) //
                .isInstanceOf(ToolAlreadyInUseByAnotherCustomerException.class) //
                .hasFieldOrPropertyWithValue("code", ToolAlreadyInUseByAnotherCustomerException.CODE);
        expectNothingSaved();
    }

    @Test
    void startToolUsage_rejectsAToolTheCustomerAlreadyUses() {
        // given
        StartToolUsageCommand command = startToolUsageCommand().build();
        ToolUsage running = givenToolInUseBy(command.customerId(), command.toolId());
        givenCustomerUsingTheTool(running);

        // when
        Throwable thrown = catchThrowable(() -> this.toolUsageService.startToolUsage(command));

        // then
        assertThat(thrown) //
                .isInstanceOf(CustomerAlreadyUsingToolException.class) //
                .hasFieldOrPropertyWithValue("code", CustomerAlreadyUsingToolException.CODE);
        expectNothingSaved();
    }

    @Test
    void startToolUsage_rejectsAThirdToolAtTheSameTime() {
        // given
        StartToolUsageCommand command = startToolUsageCommand().build();
        givenToolNotInUse(command.toolId());
        givenCustomerNotUsingTheTool(command.customerId(), command.toolId());
        givenNumberOfRunningUsages(command.customerId(), ToolUsageService.MAX_SIMULTANEOUS_TOOL_USAGES);

        // when
        Throwable thrown = catchThrowable(() -> this.toolUsageService.startToolUsage(command));

        // then
        assertThat(thrown) //
                .isInstanceOf(MaxToolUsageExceededException.class) //
                .hasFieldOrPropertyWithValue("code", MaxToolUsageExceededException.CODE);
        expectNothingSaved();
    }

    // ------------------------------------------------------------------------
    // stopToolUsage

    @Test
    void stopToolUsage_stopsTheUsageAndSavesTheEvent() {
        // given
        ToolUsage running = ToolUsage.of(startToolUsageCommand().build());
        givenCustomerUsingTheTool(running);

        // when
        this.toolUsageService.stopToolUsage(running.getCustomerId(), running.getToolId());

        // then
        assertThat(running.getEndTime()).isNotNull();
        expectSavedEvents(EventType.TOOL_USAGE_STOPPED, List.of(running));
    }

    @Test
    void stopToolUsage_rejectsAToolTheCustomerDoesNotUse() {
        // given
        CustomerId customerId = CustomerId.random();
        ToolId toolId = ToolId.random();
        givenCustomerNotUsingTheTool(customerId, toolId);

        // when
        Throwable thrown = catchThrowable(() -> this.toolUsageService.stopToolUsage(customerId, toolId));

        // then
        assertThat(thrown) //
                .isInstanceOf(ToolNotInUseByCustomerException.class) //
                .hasFieldOrPropertyWithValue("code", ToolNotInUseByCustomerException.CODE);
        expectNoEvent();
    }

    // ------------------------------------------------------------------------
    // stopToolUsages

    @Test
    void stopToolUsages_stopsEveryRunningUsageOfTheCustomerWithOneEventEach() {
        // given
        StartToolUsageCommand onFirstTool = startToolUsageCommand().build();
        ToolUsage first = ToolUsage.of(onFirstTool);
        ToolUsage second = ToolUsage.of(startToolUsageCommand().customerId(onFirstTool.customerId()).build());
        givenRunningUsagesOfTheCustomer(onFirstTool.customerId(), first, second);

        // when
        this.toolUsageService.stopToolUsages(onFirstTool.customerId());

        // then
        assertThat(first.getEndTime()).isNotNull();
        assertThat(second.getEndTime()).isNotNull();
        expectSavedEvents(EventType.TOOL_USAGE_STOPPED, List.of(first, second));
    }

    @Test
    void stopToolUsages_withoutRunningUsages_savesNoEvent() {
        // given
        CustomerId customerId = CustomerId.random();
        givenRunningUsagesOfTheCustomer(customerId);

        // when
        this.toolUsageService.stopToolUsages(customerId);

        // then
        expectNoEvent();
    }

    // ------------------------------------------------------------------------
    // given

    private void givenUsagesOfTheSession(WorkshopSessionId workshopSessionId, ToolUsage... usages) {
        when(this.toolUsageRepository.findByWorkshopSessionId(workshopSessionId)).thenReturn(List.of(usages));
    }

    private ToolUsage givenToolInUseBy(CustomerId customerId, ToolId toolId) {
        ToolUsage running = ToolUsage.of(startToolUsageCommand().customerId(customerId).toolId(toolId).build());
        when(this.toolUsageRepository.findByToolIdAndEndTimeIsNull(toolId)).thenReturn(Optional.of(running));

        return running;
    }

    private void givenToolNotInUse(ToolId toolId) {
        when(this.toolUsageRepository.findByToolIdAndEndTimeIsNull(toolId)).thenReturn(Optional.empty());
    }

    private void givenCustomerUsingTheTool(ToolUsage running) {
        when(this.toolUsageRepository.findByCustomerIdAndToolIdAndEndTimeIsNull(running.getCustomerId(), running.getToolId())) //
                .thenReturn(Optional.of(running));
    }

    private void givenRunningUsagesOfTheCustomer(CustomerId customerId, ToolUsage... usages) {
        when(this.toolUsageRepository.findByCustomerIdAndEndTimeIsNull(customerId)).thenReturn(List.of(usages));
    }

    private void givenCustomerNotUsingTheTool(CustomerId customerId, ToolId toolId) {
        when(this.toolUsageRepository.findByCustomerIdAndToolIdAndEndTimeIsNull(customerId, toolId)).thenReturn(Optional.empty());
    }

    private void givenNumberOfRunningUsages(CustomerId customerId, long count) {
        when(this.toolUsageRepository.countByCustomerIdAndEndTimeIsNull(customerId)).thenReturn(count);
    }

    // ------------------------------------------------------------------------
    // then

    private ToolUsage expectSavedUsage() {
        verify(this.toolUsageRepository).save(this.savedUsage.capture());

        return this.savedUsage.getValue();
    }

    private SchaffbarEvent expectSavedEvent(EventType type, ToolUsage usage) {
        verify(this.outboxEventStore).save(this.events.capture());
        assertThat(this.events.getValue()) //
                .singleElement() //
                .satisfies(event -> {
                    assertThat(event.getType()).isEqualTo(type);
                    assertThat(event.getAggregateId()).isEqualTo(usage.getId().getValue().toString());
                });

        return this.events.getValue().getFirst();
    }

    private void expectSavedEvents(EventType type, List<ToolUsage> usages) {
        verify(this.outboxEventStore).save(this.events.capture());
        assertThat(this.events.getValue()) //
                .extracting(SchaffbarEvent::getType, SchaffbarEvent::getAggregateId) //
                .containsExactlyElementsOf(usages.stream() //
                        .map(usage -> tuple(type, usage.getId().getValue().toString())) //
                        .toList());
    }

    private void expectNothingSaved() {
        verify(this.toolUsageRepository, never()).save(any());
        expectNoEvent();
    }

    private void expectNoEvent() {
        verifyNoInteractions(this.outboxEventStore);
    }

}
