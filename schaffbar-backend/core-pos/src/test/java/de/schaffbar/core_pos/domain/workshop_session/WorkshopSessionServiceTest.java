package de.schaffbar.core_pos.domain.workshop_session;

import static de.schaffbar.core_pos.builder.CommandBuilder.closeWorkshopSessionCommand;
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

import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionCommands.CloseWorkshopSessionCommand;
import de.schaffbar.core_pos.shared.event.EventType;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.event.outbox.OutboxEventStore;
import de.schaffbar.core_pos.shared.event.payload.WorkshopSessionEventPayload.WorkshopSessionStartedPayload;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException.Resource;
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
        expectSaved();
        WorkshopSessionStartedPayload payload = expectSavedEvent(EventType.WORKSHOP_SESSION_STARTED, workshopSessionId) //
                .deserializeAndValidatePayload(WorkshopSessionStartedPayload.class);
        assertThat(payload.id()).isEqualTo(workshopSessionId);
        assertThat(payload.customerId()).isEqualTo(customerId);
        assertThat(payload.startTime()).isNotNull();
    }

    // a guard against a bug – WorkshopUsageStart starts a session only without an open one (WS-1)
    @Test
    void startWorkshopSession_rejectsACustomerWithAnOpenSession() {
        // given
        WorkshopSession open = givenOpenSessionOfTheCustomer();

        // when
        Throwable thrown = catchThrowable(() -> this.workshopSessionService.startWorkshopSession(open.getCustomerId()));

        // then
        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        expectNothingSaved();
    }

    // ------------------------------------------------------------------------
    // closeWorkshopSession

    @Test
    void closeWorkshopSession_closesTheSessionAndSavesTheEvent() {
        // given
        WorkshopSession open = givenOpenSession();
        CloseWorkshopSessionCommand command = closeWorkshopSessionCommand(open.getId()).build();

        // when
        this.workshopSessionService.closeWorkshopSession(command);

        // then
        assertThat(open.getStatus()).isEqualTo(WorkshopSessionStatus.PAID);
        assertThat(open.getWorkshopTotal()).isEqualTo(command.workshopTotal());
        assertThat(open.getToolTotal()).isEqualTo(command.toolTotal());
        expectSavedEvent(EventType.WORKSHOP_SESSION_CLOSED, open.getId());
    }

    @Test
    void closeWorkshopSession_rejectsAnUnknownSession() {
        // given
        WorkshopSessionId unknown = givenUnknownSession();

        // when
        Throwable thrown = catchThrowable(() -> this.workshopSessionService.closeWorkshopSession(closeWorkshopSessionCommand(unknown).build()));

        // then
        assertThat(thrown) //
                .isInstanceOf(ResourceNotFoundException.class) //
                .hasFieldOrPropertyWithValue("resource", Resource.WORKSHOP_SESSION);
        expectNoEvent();
    }

    // ------------------------------------------------------------------------
    // given

    private WorkshopSession givenOpenSession() {
        WorkshopSession open = WorkshopSession.of(CustomerId.random());
        when(this.workshopSessionRepository.findById(open.getId().getValue())).thenReturn(Optional.of(open));

        return open;
    }

    private WorkshopSession givenOpenSessionOfTheCustomer() {
        WorkshopSession open = WorkshopSession.of(CustomerId.random());
        when(this.workshopSessionRepository.findOpenWorkshopSession(open.getCustomerId())).thenReturn(Optional.of(open));

        return open;
    }

    private WorkshopSessionId givenUnknownSession() {
        WorkshopSessionId unknown = WorkshopSessionId.random();
        when(this.workshopSessionRepository.findById(unknown.getValue())).thenReturn(Optional.empty());

        return unknown;
    }

    private CustomerId givenCustomerWithoutOpenSession() {
        CustomerId customerId = CustomerId.random();
        when(this.workshopSessionRepository.findOpenWorkshopSession(customerId)).thenReturn(Optional.empty());

        return customerId;
    }

    // ------------------------------------------------------------------------
    // then

    private void expectSaved() {
        verify(this.workshopSessionRepository).save(any());
    }

    private void expectNothingSaved() {
        verify(this.workshopSessionRepository, never()).save(any());
        expectNoEvent();
    }

    private SchaffbarEvent expectSavedEvent(EventType type, WorkshopSessionId workshopSessionId) {
        verify(this.outboxEventStore).save(this.events.capture());
        assertThat(this.events.getValue()) //
                .singleElement() //
                .satisfies(event -> {
                    assertThat(event.getType()).isEqualTo(type);
                    assertThat(event.getAggregateId()).isEqualTo(workshopSessionId.getValue().toString());
                });

        return this.events.getValue().getFirst();
    }

    private void expectNoEvent() {
        verifyNoInteractions(this.outboxEventStore);
    }

}
