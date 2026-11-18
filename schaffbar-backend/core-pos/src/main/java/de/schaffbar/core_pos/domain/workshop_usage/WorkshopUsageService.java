package de.schaffbar.core_pos.domain.workshop_usage;

import static java.util.Comparator.comparing;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageViews.WorkshopUsageView;
import de.schaffbar.core_pos.shared.billing.BillableDuration;
import de.schaffbar.core_pos.shared.billing.BillingTotal;
import de.schaffbar.core_pos.shared.cqrs.Command;
import de.schaffbar.core_pos.shared.cqrs.Query;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.event.outbox.OutboxEventStore;
import de.schaffbar.core_pos.shared.exception.CustomerAlreadyInWorkshopException;
import de.schaffbar.core_pos.shared.exception.NoActiveWorkshopUsageFoundException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class WorkshopUsageService {

    private final @NonNull WorkshopUsageRepository workshopUsageRepository;

    private final @NonNull OutboxEventStore outboxEventStore;

    // ------------------------------------------------------------------------
    // query

    @Query
    public List<WorkshopUsageView> getWorkshopUsages(@NotNull @Valid WorkshopSessionId workshopSessionId) {
        return this.workshopUsageRepository.findByWorkshopSessionId(workshopSessionId).stream() //
                .map(WorkshopUsageViews.MAPPER::toWorkshopUsageView) //
                .sorted(comparing(WorkshopUsageView::entryTime)) //
                .toList();
    }

    @Query
    public Optional<WorkshopUsageView> getActiveWorkshopUsage(@NotNull @Valid CustomerId customerId) {
        return this.workshopUsageRepository.findByCustomerIdAndExitTimeIsNull(customerId) //
                .map(WorkshopUsageViews.MAPPER::toWorkshopUsageView);
    }

    @Query
    public List<WorkshopUsageView> getActiveWorkshopUsages() {
        return this.workshopUsageRepository.findByExitTimeIsNull().stream() //
                .map(WorkshopUsageViews.MAPPER::toWorkshopUsageView) //
                .sorted(comparing(WorkshopUsageView::entryTime)) //
                .toList();
    }

    // TODO: create two separate methods: one finalBillingTotal (without Instant) and one ...BillingTotal (with Instant)
    @Query
    public BillingTotal getBillingTotalUntil(@NotNull @Valid WorkshopSessionId workshopSessionId, @NotNull Instant now) {
        List<BillableDuration> durationList = this.workshopUsageRepository.findByWorkshopSessionId(workshopSessionId).stream() //
                .map(usage -> usage.getBillableDurationUntil(now)) //
                .toList();

        return BillingTotal.of(durationList);
    }

    // ------------------------------------------------------------------------
    // command

    @Command
    public void startWorkshopUsage(@NotNull @Valid CustomerId customerId, @NotNull @Valid WorkshopSessionId workshopSessionId) {
        ensureCustomerNotInWorkshop(customerId);

        WorkshopUsage workshopUsage = WorkshopUsage.of(customerId, workshopSessionId);
        this.workshopUsageRepository.save(workshopUsage);
        List<SchaffbarEvent> events = List.of(WorkshopUsageEventFactory.workshopUsageEntered(workshopUsage));

        this.outboxEventStore.save(events);

        log.info("Customer {} entered workshop and published events {}", customerId, events);
    }

    @Command
    public void stopWorkshopUsage(@NotNull @Valid CustomerId customerId) {
        WorkshopUsage workshopUsage = this.workshopUsageRepository.findByCustomerIdAndExitTimeIsNull(customerId) //
                .orElseThrow(() -> new NoActiveWorkshopUsageFoundException(customerId));

        List<SchaffbarEvent> events = workshopUsage.exit();

        this.outboxEventStore.save(events);

        log.info("Customer {} left workshop and published events {}", customerId, events);
    }

    // ------------------------------------------------------------------------
    // helper

    // WU-1
    private void ensureCustomerNotInWorkshop(CustomerId customerId) {
        this.workshopUsageRepository.findByCustomerIdAndExitTimeIsNull(customerId) //
                .ifPresent(this::throwCustomerAlreadyInWorkshopException);
    }

    private void throwCustomerAlreadyInWorkshopException(WorkshopUsage active) {
        throw new CustomerAlreadyInWorkshopException(active.getCustomerId());
    }

}
