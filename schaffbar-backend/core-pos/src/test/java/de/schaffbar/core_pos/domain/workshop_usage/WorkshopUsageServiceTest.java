package de.schaffbar.core_pos.domain.workshop_usage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.schaffbar.core_pos.shared.billing.BillingTotal;
import de.schaffbar.core_pos.shared.event.EventType;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.event.outbox.OutboxEventStore;
import de.schaffbar.core_pos.shared.event.payload.WorkshopUsageEventPayload.WorkshopUsageEnteredPayload;
import de.schaffbar.core_pos.shared.exception.CustomerAlreadyInWorkshopException;
import de.schaffbar.core_pos.shared.exception.NoRunningWorkshopUsageFoundException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;

@ExtendWith(MockitoExtension.class)
class WorkshopUsageServiceTest {

    @Mock
    private WorkshopUsageRepository workshopUsageRepository;

    @Mock
    private OutboxEventStore outboxEventStore;

    @Captor
    private ArgumentCaptor<List<SchaffbarEvent>> events;

    @Captor
    private ArgumentCaptor<WorkshopUsage> savedUsage;

    @InjectMocks
    private WorkshopUsageService workshopUsageService;

    // ------------------------------------------------------------------------
    // getBillingTotal

    @Test
    void getBillingTotal_addsTheFinishedUsagesOfTheSession() {
        // given – checked out twice
        WorkshopSessionId workshopSessionId = WorkshopSessionId.random();
        CustomerId customerId = CustomerId.random();
        WorkshopUsage first = WorkshopUsage.of(customerId, workshopSessionId);
        first.stop();
        WorkshopUsage second = WorkshopUsage.of(customerId, workshopSessionId);
        second.stop();
        givenUsagesOfTheSession(workshopSessionId, first, second);

        // when
        BillingTotal total = this.workshopUsageService.getBillingTotal(workshopSessionId);

        // then
        assertThat(total).isEqualTo(BillingTotal.of(List.of(first.getBillableDuration(), second.getBillableDuration())));
    }

    // a guard against a bug – WorkshopSessionClose checks before that no visit is running (WS-5)
    @Test
    void getBillingTotal_rejectsASessionWithARunningUsage() {
        // given
        WorkshopSessionId workshopSessionId = WorkshopSessionId.random();
        givenAFinishedAndARunningUsageOfTheSession(workshopSessionId);

        // when
        Throwable thrown = catchThrowable(() -> this.workshopUsageService.getBillingTotal(workshopSessionId));

        // then
        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }

    // ------------------------------------------------------------------------
    // getBillingTotalUntil

    @Test
    void getBillingTotalUntil_countsAFinishedUsageUntilItsExitAndTheRunningOneUntilNow() {
        // given – checked out once and checked in again
        WorkshopSessionId workshopSessionId = WorkshopSessionId.random();
        List<WorkshopUsage> usages = givenAFinishedAndARunningUsageOfTheSession(workshopSessionId);
        WorkshopUsage finished = usages.getFirst();
        WorkshopUsage running = usages.getLast();
        Instant now = Instant.now().plus(Duration.ofMinutes(13).plusSeconds(1));

        // when
        BillingTotal total = this.workshopUsageService.getBillingTotalUntil(workshopSessionId, now);

        // then – every usage is rounded on its own
        assertThat(total).isEqualTo(BillingTotal.of(List.of(finished.getBillableDuration(), running.getBillableDurationUntil(now))));
        assertThat(running.getBillableDurationUntil(now).getMinutes()).isEqualTo(14);
        assertThat(finished.getBillableDuration().getMinutes()).isLessThanOrEqualTo(1);
    }

    // ------------------------------------------------------------------------
    // startWorkshopUsage

    @Test
    void startWorkshopUsage_savesTheUsageAndItsEnteredEvent() {
        // given
        CustomerId customerId = givenCustomerNotInTheWorkshop();
        WorkshopSessionId workshopSessionId = WorkshopSessionId.random();

        // when
        this.workshopUsageService.startWorkshopUsage(customerId, workshopSessionId);

        // then
        WorkshopUsage usage = expectSavedUsage();
        WorkshopUsageEnteredPayload payload = expectSavedEvent(EventType.WORKSHOP_USAGE_ENTERED, usage) //
                .deserializeAndValidatePayload(WorkshopUsageEnteredPayload.class);
        assertThat(payload.id()).isEqualTo(usage.getId());
        assertThat(payload.customerId()).isEqualTo(customerId);
        assertThat(payload.workshopSessionId()).isEqualTo(workshopSessionId);
        assertThat(payload.entryTime()).isNotNull();
    }

    @Test
    void startWorkshopUsage_rejectsACustomerInTheWorkshop() {
        // given
        WorkshopUsage running = givenCustomerInTheWorkshop();

        // when
        Throwable thrown = catchThrowable(() -> this.workshopUsageService.startWorkshopUsage(running.getCustomerId(), WorkshopSessionId.random()));

        // then
        assertThat(thrown) //
                .isInstanceOf(CustomerAlreadyInWorkshopException.class) //
                .hasFieldOrPropertyWithValue("code", CustomerAlreadyInWorkshopException.CODE);
        expectNothingSaved();
    }

    // ------------------------------------------------------------------------
    // stopWorkshopUsage

    @Test
    void stopWorkshopUsage_endsTheUsageAndSavesTheEvent() {
        // given
        WorkshopUsage running = givenCustomerInTheWorkshop();

        // when
        this.workshopUsageService.stopWorkshopUsage(running.getCustomerId());

        // then
        assertThat(running.getExitTime()).isNotNull();
        expectSavedEvent(EventType.WORKSHOP_USAGE_LEFT, running);
    }

    @Test
    void stopWorkshopUsage_rejectsACustomerWhoIsNotInTheWorkshop() {
        // given
        CustomerId customerId = givenCustomerNotInTheWorkshop();

        // when
        Throwable thrown = catchThrowable(() -> this.workshopUsageService.stopWorkshopUsage(customerId));

        // then
        assertThat(thrown) //
                .isInstanceOf(NoRunningWorkshopUsageFoundException.class) //
                .hasFieldOrPropertyWithValue("code", NoRunningWorkshopUsageFoundException.CODE);
        expectNoEvent();
    }

    // ------------------------------------------------------------------------
    // given

    private WorkshopUsage givenCustomerInTheWorkshop() {
        WorkshopUsage running = WorkshopUsage.of(CustomerId.random(), WorkshopSessionId.random());
        when(this.workshopUsageRepository.findByCustomerIdAndExitTimeIsNull(running.getCustomerId())).thenReturn(Optional.of(running));

        return running;
    }

    private CustomerId givenCustomerNotInTheWorkshop() {
        CustomerId customerId = CustomerId.random();
        when(this.workshopUsageRepository.findByCustomerIdAndExitTimeIsNull(customerId)).thenReturn(Optional.empty());

        return customerId;
    }

    private void givenUsagesOfTheSession(WorkshopSessionId workshopSessionId, WorkshopUsage... usages) {
        when(this.workshopUsageRepository.findByWorkshopSessionId(workshopSessionId)).thenReturn(List.of(usages));
    }

    private List<WorkshopUsage> givenAFinishedAndARunningUsageOfTheSession(WorkshopSessionId workshopSessionId) {
        CustomerId customerId = CustomerId.random();
        WorkshopUsage finished = WorkshopUsage.of(customerId, workshopSessionId);
        finished.stop();
        WorkshopUsage running = WorkshopUsage.of(customerId, workshopSessionId);
        List<WorkshopUsage> usages = List.of(finished, running);
        when(this.workshopUsageRepository.findByWorkshopSessionId(workshopSessionId)).thenReturn(usages);

        return usages;
    }

    // ------------------------------------------------------------------------
    // then

    private WorkshopUsage expectSavedUsage() {
        verify(this.workshopUsageRepository).save(this.savedUsage.capture());

        return this.savedUsage.getValue();
    }

    private void expectNothingSaved() {
        verify(this.workshopUsageRepository, never()).save(any());
        expectNoEvent();
    }

    private SchaffbarEvent expectSavedEvent(EventType type, WorkshopUsage usage) {
        verify(this.outboxEventStore).save(this.events.capture());
        assertThat(this.events.getValue()) //
                .singleElement() //
                .satisfies(event -> {
                    assertThat(event.getType()).isEqualTo(type);
                    assertThat(event.getAggregateId()).isEqualTo(usage.getId().getValue().toString());
                });

        return this.events.getValue().getFirst();
    }

    private void expectNoEvent() {
        verifyNoInteractions(this.outboxEventStore);
    }

}
