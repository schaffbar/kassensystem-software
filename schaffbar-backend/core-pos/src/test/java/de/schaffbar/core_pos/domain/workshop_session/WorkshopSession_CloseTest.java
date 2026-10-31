package de.schaffbar.core_pos.domain.workshop_session;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.schaffbar.core_pos.shared.billing.BillingTotal;
import de.schaffbar.core_pos.shared.event.EventType;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.event.payload.WorkshopSessionEventPayload.WorkshopSessionClosedPayload;
import de.schaffbar.core_pos.shared.id.CustomerId;

class WorkshopSession_CloseTest {

    @Test
    void hasNoTotalsWhileOpen() {
        // given
        WorkshopSession session = workshopSession();

        // when / then
        assertThat(session.getStatus()).isEqualTo(WorkshopSessionStatus.OPEN);
        assertThat(session.getWorkshopTotal()).isNull();
        assertThat(session.getToolTotal()).isNull();
    }

    @Test
    void storesTheFinalTotalsWhenClosed() {
        // given
        WorkshopSession session = workshopSession();
        BillingTotal workshopTotal = new BillingTotal(31, 6);
        BillingTotal toolTotal = new BillingTotal(7, 2);

        // when
        List<SchaffbarEvent> events = session.close(workshopTotal, toolTotal);

        // then
        assertThat(session.getStatus()).isEqualTo(WorkshopSessionStatus.PAID);
        assertThat(session.getCloseTime()).isNotNull();
        assertThat(session.getWorkshopTotal()).isEqualTo(workshopTotal);
        assertThat(session.getToolTotal()).isEqualTo(toolTotal);
        assertThat(events).hasSize(1);
    }

    @Test
    void publishesTheTotalsWithTheClosedEvent() {
        // given
        WorkshopSession session = workshopSession();
        BillingTotal workshopTotal = new BillingTotal(31, 6);
        BillingTotal toolTotal = new BillingTotal(7, 2);

        // when
        SchaffbarEvent event = session.close(workshopTotal, toolTotal).getFirst();

        // then – read back from the JSON payload, as a consumer would
        WorkshopSessionClosedPayload payload = event.deserializeAndValidatePayload(WorkshopSessionClosedPayload.class);
        assertThat(event.getType()).isEqualTo(EventType.WORKSHOP_SESSION_CLOSED);
        assertThat(payload.closeTime()).isEqualTo(session.getCloseTime());
        assertThat(payload.workshopTotal()).isEqualTo(workshopTotal);
        assertThat(payload.toolTotal()).isEqualTo(toolTotal);
    }

    @Test
    void storesZeroTotalsForASessionWithoutUsages() {
        // given
        WorkshopSession session = workshopSession();

        // when
        session.close(BillingTotal.ZERO, BillingTotal.ZERO);

        // then – zero, not null: the session was billed, there was just nothing to bill
        assertThat(session.getWorkshopTotal()).isEqualTo(BillingTotal.ZERO);
        assertThat(session.getToolTotal()).isEqualTo(BillingTotal.ZERO);
    }

    // ------------------------------------------------------------------------
    // helper

    private WorkshopSession workshopSession() {
        return WorkshopSession.of(CustomerId.random());
    }

}
