package de.schaffbar.core_pos.domain.workshop_usage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import de.schaffbar.core_pos.integration.IntegrationTest;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;

// what only the database shows: the partial unique index of V9 – the service checks WU-1 before, but two check-ins
// at the same time can both pass that check
@IntegrationTest
class WorkshopUsageRepositoryIntegrationTest {

    @Autowired
    private WorkshopUsageRepository workshopUsageRepository;

    @Test
    void aCustomerHasAtMostOneRunningWorkshopUsage() {
        // given
        CustomerId customerId = CustomerId.random();
        WorkshopSessionId workshopSessionId = WorkshopSessionId.random();
        WorkshopUsage running = givenRunningWorkshopUsage(customerId, workshopSessionId);

        // when
        Throwable thrown = catchThrowable(() -> this.workshopUsageRepository.saveAndFlush(WorkshopUsage.of(customerId, workshopSessionId)));

        // then
        assertThat(thrown).isInstanceOf(DataIntegrityViolationException.class);

        // when – after checking out
        running.stop();
        this.workshopUsageRepository.saveAndFlush(running);
        WorkshopUsage next = this.workshopUsageRepository.saveAndFlush(WorkshopUsage.of(customerId, workshopSessionId));

        // then
        assertThat(this.workshopUsageRepository.findById(next.getId().getValue())).isPresent();
    }

    // ------------------------------------------------------------------------
    // given

    private WorkshopUsage givenRunningWorkshopUsage(CustomerId customerId, WorkshopSessionId workshopSessionId) {
        return this.workshopUsageRepository.saveAndFlush(WorkshopUsage.of(customerId, workshopSessionId));
    }

}
