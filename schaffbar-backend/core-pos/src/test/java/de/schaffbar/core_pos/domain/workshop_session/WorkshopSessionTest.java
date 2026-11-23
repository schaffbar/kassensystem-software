package de.schaffbar.core_pos.domain.workshop_session;

import static de.schaffbar.core_pos.builder.CommandBuilder.closeWorkshopSessionCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionCommands.CloseWorkshopSessionCommand;
import de.schaffbar.core_pos.shared.billing.BillingTotal;
import de.schaffbar.core_pos.shared.event.EventType;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.event.payload.WorkshopSessionEventPayload.WorkshopSessionClosedPayload;
import de.schaffbar.core_pos.shared.id.CustomerId;

class WorkshopSessionTest {

    // ------------------------------------------------------------------------
    // of

    @Test
    void of_startsAnOpenSessionWithoutTotals() {
        // given
        CustomerId customerId = CustomerId.random();

        // when
        WorkshopSession session = WorkshopSession.of(customerId);

        // then
        assertThat(session.getId()).isNotNull();
        assertThat(session.getCustomerId()).isEqualTo(customerId);
        assertThat(session.getStartTime()).isNotNull();
        assertThat(session.getStatus()).isEqualTo(WorkshopSessionStatus.OPEN);
        assertThat(session.getCloseTime()).isNull();
        assertThat(session.getWorkshopTotal()).isNull();
        assertThat(session.getToolTotal()).isNull();
    }

    // ------------------------------------------------------------------------
    // close

    @Test
    void close_storesTheFinalTotalsAndReturnsTheEvent() {
        // given
        WorkshopSession session = givenOpenSession();
        CloseWorkshopSessionCommand command = closeWorkshopSessionCommand(session.getId()).build();

        // when
        List<SchaffbarEvent> events = session.close(command);

        // then
        assertThat(session.getStatus()).isEqualTo(WorkshopSessionStatus.PAID);
        assertThat(session.getCloseTime()).isAfterOrEqualTo(session.getStartTime());
        assertThat(session.getWorkshopTotal()).isEqualTo(command.workshopTotal());
        assertThat(session.getToolTotal()).isEqualTo(command.toolTotal());

        WorkshopSessionClosedPayload payload = expectSingleEvent(events, EventType.WORKSHOP_SESSION_CLOSED, session) //
                .deserializeAndValidatePayload(WorkshopSessionClosedPayload.class);
        assertThat(payload.id()).isEqualTo(session.getId());
        assertThat(payload.customerId()).isEqualTo(session.getCustomerId());
        assertThat(payload.startTime()).isEqualTo(session.getStartTime());
        assertThat(payload.closeTime()).isEqualTo(session.getCloseTime());
        assertThat(payload.workshopTotal()).isEqualTo(command.workshopTotal());
        assertThat(payload.toolTotal()).isEqualTo(command.toolTotal());
    }

    @Test
    void close_withoutUsages_storesZeroTotals() {
        // given
        WorkshopSession session = givenOpenSession();
        CloseWorkshopSessionCommand command = closeWorkshopSessionCommand(session.getId()) //
                .workshopTotal(BillingTotal.ZERO) //
                .toolTotal(BillingTotal.ZERO) //
                .build();

        // when
        session.close(command);

        // then – zero, not null: the session was billed, there was just nothing to bill
        assertThat(session.getWorkshopTotal()).isEqualTo(BillingTotal.ZERO);
        assertThat(session.getToolTotal()).isEqualTo(BillingTotal.ZERO);
    }

    // a guard against a bug – WorkshopSessionClose finds only an open session (WS-4)
    @Test
    void close_rejectsAClosedSession() {
        // given
        WorkshopSession session = givenClosedSession();
        Instant closeTime = session.getCloseTime();
        BillingTotal workshopTotal = session.getWorkshopTotal();

        // when
        Throwable thrown = catchThrowable(() -> session.close(closeWorkshopSessionCommand(session.getId()).build()));

        // then
        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        assertThat(session.getCloseTime()).isEqualTo(closeTime);
        assertThat(session.getWorkshopTotal()).isEqualTo(workshopTotal);
    }

    // ------------------------------------------------------------------------
    // given

    private WorkshopSession givenOpenSession() {
        return WorkshopSession.of(CustomerId.random());
    }

    private WorkshopSession givenClosedSession() {
        WorkshopSession session = givenOpenSession();
        session.close(closeWorkshopSessionCommand(session.getId()).build());

        return session;
    }

    // ------------------------------------------------------------------------
    // then

    private SchaffbarEvent expectSingleEvent(List<SchaffbarEvent> events, EventType type, WorkshopSession session) {
        assertThat(events).hasSize(1);
        SchaffbarEvent event = events.getFirst();
        assertThat(event.getType()).isEqualTo(type);
        assertThat(event.getAggregateId()).isEqualTo(session.getId().getValue().toString());

        return event;
    }

}
