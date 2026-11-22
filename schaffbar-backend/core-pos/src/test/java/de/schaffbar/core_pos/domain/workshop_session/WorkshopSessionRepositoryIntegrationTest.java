package de.schaffbar.core_pos.domain.workshop_session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import de.schaffbar.core_pos.integration.IntegrationTest;
import de.schaffbar.core_pos.shared.billing.BillingTotal;
import de.schaffbar.core_pos.shared.id.CustomerId;

// what only the database shows: the partial unique index of V9 – the service checks WS-1 before, but two first
// check-ins at the same time can both pass that check
@IntegrationTest
class WorkshopSessionRepositoryIntegrationTest {

    @Autowired
    private WorkshopSessionRepository workshopSessionRepository;

    @Test
    void aCustomerHasAtMostOneOpenWorkshopSession() {
        // given
        CustomerId customerId = CustomerId.random();
        WorkshopSession open = givenOpenWorkshopSession(customerId);

        // when
        Throwable thrown = catchThrowable(() -> this.workshopSessionRepository.saveAndFlush(WorkshopSession.of(customerId)));

        // then
        assertThat(thrown).isInstanceOf(DataIntegrityViolationException.class);

        // when – after billing
        open.close(BillingTotal.ZERO, BillingTotal.ZERO);
        this.workshopSessionRepository.saveAndFlush(open);
        WorkshopSession next = this.workshopSessionRepository.saveAndFlush(WorkshopSession.of(customerId));

        // then
        assertThat(this.workshopSessionRepository.findById(next.getId().getValue())).isPresent();
    }

    // ------------------------------------------------------------------------
    // given

    private WorkshopSession givenOpenWorkshopSession(CustomerId customerId) {
        return this.workshopSessionRepository.saveAndFlush(WorkshopSession.of(customerId));
    }

}
