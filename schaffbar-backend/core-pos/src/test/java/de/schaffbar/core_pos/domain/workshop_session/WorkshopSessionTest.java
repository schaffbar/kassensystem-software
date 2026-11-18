package de.schaffbar.core_pos.domain.workshop_session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

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
        BillingTotal workshopTotal = new BillingTotal(31, 6);
        BillingTotal toolTotal = new BillingTotal(7, 2);

        // when
        List<SchaffbarEvent> events = session.close(workshopTotal, toolTotal);

        // then
        assertThat(session.getStatus()).isEqualTo(WorkshopSessionStatus.PAID);
        assertThat(session.getCloseTime()).isAfterOrEqualTo(session.getStartTime());
        assertThat(session.getWorkshopTotal()).isEqualTo(workshopTotal);
        assertThat(session.getToolTotal()).isEqualTo(toolTotal);

        assertThat(events).hasSize(1);
        SchaffbarEvent event = events.getFirst();
        assertThat(event.getType()).isEqualTo(EventType.WORKSHOP_SESSION_CLOSED);
        assertThat(event.getAggregateId()).isEqualTo(session.getId().getValue().toString());

        WorkshopSessionClosedPayload payload = event.deserializeAndValidatePayload(WorkshopSessionClosedPayload.class);
        assertThat(payload.id()).isEqualTo(session.getId());
        assertThat(payload.customerId()).isEqualTo(session.getCustomerId());
        assertThat(payload.startTime()).isEqualTo(session.getStartTime());
        assertThat(payload.closeTime()).isEqualTo(session.getCloseTime());
        assertThat(payload.workshopTotal()).isEqualTo(workshopTotal);
        assertThat(payload.toolTotal()).isEqualTo(toolTotal);
    }

    @Test
    void close_withoutUsages_storesZeroTotals() {
        // given
        WorkshopSession session = givenOpenSession();

        // when
        session.close(BillingTotal.ZERO, BillingTotal.ZERO);

        // then – zero, not null: the session was billed, there was just nothing to bill
        assertThat(session.getWorkshopTotal()).isEqualTo(BillingTotal.ZERO);
        assertThat(session.getToolTotal()).isEqualTo(BillingTotal.ZERO);
    }

    @Test
    void close_rejectsAClosedSession() {
        // given
        WorkshopSession session = givenOpenSession();
        BillingTotal workshopTotal = new BillingTotal(31, 6);
        session.close(workshopTotal, BillingTotal.ZERO);
        Instant closeTime = session.getCloseTime();

        // when
        Throwable thrown = catchThrowable(() -> session.close(new BillingTotal(60, 10), BillingTotal.ZERO));

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

}
