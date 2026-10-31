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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageViews.WorkshopUsageView;
import de.schaffbar.core_pos.shared.billing.BillableDuration;
import de.schaffbar.core_pos.shared.billing.BillingTotal;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.event.outbox.OutboxEvent;
import de.schaffbar.core_pos.shared.event.outbox.OutboxEventRepository;
import de.schaffbar.core_pos.shared.exception.NoActiveWorkshopUsageFoundException;
import de.schaffbar.core_pos.shared.exception.UserAlreadyInWorkshopException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class WorkshopUsageService {

    private final @NonNull WorkshopUsageRepository workshopUsageRepository;

    private final @NonNull OutboxEventRepository outboxEventRepository;

    // ------------------------------------------------------------------------
    // query

    public List<WorkshopUsageView> getWorkshopUsages(@NotNull @Valid WorkshopSessionId workshopSessionId) {
        return this.workshopUsageRepository.findWorkshopUsages(workshopSessionId).stream() //
                .map(WorkshopUsageViews.MAPPER::toWorkshopUsageView) //
                .sorted(comparing(WorkshopUsageView::entryTime)) //
                .toList();
    }

    public List<WorkshopUsageView> getAllActiveWorkshopUsages() {
        return this.workshopUsageRepository.findByExitTimeIsNull().stream() //
                .map(WorkshopUsageViews.MAPPER::toWorkshopUsageView) //
                .sorted(comparing(WorkshopUsageView::entryTime)) //
                .toList();
    }

    // TODO: create two separate methods: one finalBillingTotal (without Instant) and one ...BillingTotal (with Instant)
    public BillingTotal getBillingTotal(@NotNull @Valid WorkshopSessionId workshopSessionId, @NotNull Instant now) {
        List<BillableDuration> durationList = this.workshopUsageRepository.findWorkshopUsages(workshopSessionId).stream() //
                .map(usage -> usage.getBillableDurationUntil(now)) //
                .toList();

        return BillingTotal.of(durationList);
    }

    // ------------------------------------------------------------------------
    // command

    @Transactional
    public void enterWorkshop(@NotNull @Valid CustomerId customerId, @NotNull @Valid WorkshopSessionId workshopSessionId) {
        getOpenWorkshopUsage(customerId, workshopSessionId) //
                .ifPresent(this::throwCustomerIsAlreadyInWorkshop);

        WorkshopUsage workshopUsage = WorkshopUsage.of(customerId, workshopSessionId);
        WorkshopUsage savedWorkshopUsage = this.workshopUsageRepository.save(workshopUsage);
        List<SchaffbarEvent> events = List.of(WorkshopUsageEventFactory.workshopUsageEntered(savedWorkshopUsage));

        saveOutboxEvents(events);

        log.info("Customer {} entered workshop and published events {}", customerId, events);
    }

    @Transactional
    public void leaveWorkshop(@NotNull @Valid CustomerId customerId, @NotNull @Valid WorkshopSessionId workshopSessionId) {
        WorkshopUsage usage = getOpenWorkshopUsage(customerId, workshopSessionId) //
                .orElseThrow(() -> new NoActiveWorkshopUsageFoundException(customerId));

        List<SchaffbarEvent> events = usage.exit();
        saveOutboxEvents(events);

        log.info("Customer {} left workshop and published events {}", customerId, events);
    }

    // ------------------------------------------------------------------------
    // helper

    // TODO: decide what to use customerId, workshopSessionId or both
    private Optional<WorkshopUsage> getOpenWorkshopUsage(CustomerId customerId, WorkshopSessionId workshopSessionId) {
        return this.workshopUsageRepository.findActiveByCustomerId(customerId);
    }

    private void throwCustomerIsAlreadyInWorkshop(WorkshopUsage workshopUsage) {
        throw new UserAlreadyInWorkshopException(workshopUsage.getCustomerId());
    }

    private void saveOutboxEvents(List<SchaffbarEvent> events) {
        List<OutboxEvent> outboxEvents = events.stream() //
                .map(OutboxEvent::of) //
                .toList();

        this.outboxEventRepository.saveAll(outboxEvents);
    }

}
