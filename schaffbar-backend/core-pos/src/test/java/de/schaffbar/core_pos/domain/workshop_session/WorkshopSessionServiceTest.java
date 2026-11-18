package de.schaffbar.core_pos.domain.workshop_session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import de.schaffbar.core_pos.shared.event.payload.WorkshopSessionEventPayload.WorkshopSessionStartedPayload;
import de.schaffbar.core_pos.shared.exception.NoOpenWorkshopSessionException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;

@ExtendWith(MockitoExtension.class)
class WorkshopSessionServiceTest {

    @Mock
    private WorkshopSessionRepository workshopSessionRepository;

    @Mock
    private OutboxEventStore outboxEventStore;

    @Captor
    private ArgumentCaptor<List<SchaffbarEvent>> events;

    @InjectMocks
    private WorkshopSessionService workshopSessionService;

    // ------------------------------------------------------------------------
    // startWorkshopSession

    @Test
    void startWorkshopSession_savesTheSessionAndItsStartedEvent() {
        // given
        CustomerId customerId = givenCustomerWithoutOpenSession();

        // when
        WorkshopSessionId workshopSessionId = this.workshopSessionService.startWorkshopSession(customerId);

        // then
        verify(this.workshopSessionRepository).save(any());
        WorkshopSessionStartedPayload payload = expectSavedEvent(EventType.WORKSHOP_SESSION_STARTED) //
                .deserializeAndValidatePayload(WorkshopSessionStartedPayload.class);
        assertThat(payload.id()).isEqualTo(workshopSessionId);
        assertThat(payload.customerId()).isEqualTo(customerId);
        assertThat(payload.startTime()).isNotNull();
    }

    @Test
    void startWorkshopSession_rejectsACustomerWithAnOpenSession() {
        // given
        WorkshopSession open = givenOpenSession();

        // when
        Throwable thrown = catchThrowable(() -> this.workshopSessionService.startWorkshopSession(open.getCustomerId()));

        // then
        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        verify(this.workshopSessionRepository, never()).save(any());
        expectNoEvent();
    }

    // ------------------------------------------------------------------------
    // closeWorkshopSession

    @Test
    void closeWorkshopSession_closesTheOpenSessionAndSavesTheEvent() {
        // given
        WorkshopSession open = givenOpenSession();
        BillingTotal workshopTotal = new BillingTotal(31, 6);
        BillingTotal toolTotal = new BillingTotal(7, 2);

        // when
        this.workshopSessionService.closeWorkshopSession(open.getCustomerId(), workshopTotal, toolTotal);

        // then
        assertThat(open.getStatus()).isEqualTo(WorkshopSessionStatus.PAID);
        assertThat(open.getWorkshopTotal()).isEqualTo(workshopTotal);
        assertThat(open.getToolTotal()).isEqualTo(toolTotal);
        SchaffbarEvent event = expectSavedEvent(EventType.WORKSHOP_SESSION_CLOSED);
        assertThat(event.getAggregateId()).isEqualTo(open.getId().getValue().toString());
    }

    @Test
    void closeWorkshopSession_rejectsACustomerWithoutOpenSession() {
        // given
        CustomerId customerId = givenCustomerWithoutOpenSession();

        // when
        Throwable thrown = catchThrowable(() -> this.workshopSessionService.closeWorkshopSession(customerId, BillingTotal.ZERO, BillingTotal.ZERO));

        // then
        assertThat(thrown) //
                .isInstanceOf(NoOpenWorkshopSessionException.class) //
                .hasFieldOrPropertyWithValue("code", NoOpenWorkshopSessionException.CODE);
        expectNoEvent();
    }

    // ------------------------------------------------------------------------
    // given

    private WorkshopSession givenOpenSession() {
        WorkshopSession open = WorkshopSession.of(CustomerId.random());
        when(this.workshopSessionRepository.findOpenWorkshopSession(open.getCustomerId())).thenReturn(Optional.of(open));

        return open;
    }

    private CustomerId givenCustomerWithoutOpenSession() {
        CustomerId customerId = CustomerId.random();
        when(this.workshopSessionRepository.findOpenWorkshopSession(customerId)).thenReturn(Optional.empty());

        return customerId;
    }

    // ------------------------------------------------------------------------
    // then

    private SchaffbarEvent expectSavedEvent(EventType type) {
        verify(this.outboxEventStore).save(this.events.capture());
        assertThat(this.events.getValue()) //
                .singleElement() //
                .satisfies(event -> assertThat(event.getType()).isEqualTo(type));

        return this.events.getValue().getFirst();
    }

    private void expectNoEvent() {
        verifyNoInteractions(this.outboxEventStore);
    }

}
