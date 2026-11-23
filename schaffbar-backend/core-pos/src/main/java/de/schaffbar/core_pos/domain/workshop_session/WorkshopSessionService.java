package de.schaffbar.core_pos.domain.workshop_session;

import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionCommands.CloseWorkshopSessionCommand;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionViews.WorkshopSessionView;
import de.schaffbar.core_pos.shared.cqrs.Command;
import de.schaffbar.core_pos.shared.cqrs.Query;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.event.outbox.OutboxEventStore;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class WorkshopSessionService {

    private final @NonNull WorkshopSessionRepository workshopSessionRepository;

    private final @NonNull OutboxEventStore outboxEventStore;

    // ------------------------------------------------------------------------
    // query

    @Query
    public Optional<WorkshopSessionView> getOpenWorkshopSession(@NotNull @Valid CustomerId customerId) {
        return this.workshopSessionRepository.findOpenWorkshopSession(customerId) //
                .map(WorkshopSessionViews.MAPPER::toWorkshopSessionView);
    }

    // ------------------------------------------------------------------------
    // command

    @Command
    public WorkshopSessionId startWorkshopSession(@NotNull @Valid CustomerId customerId) {
        ensureNoOpenWorkshopSession(customerId);

        WorkshopSession workshopSession = WorkshopSession.of(customerId);
        this.workshopSessionRepository.save(workshopSession);
        List<SchaffbarEvent> events = List.of(WorkshopSessionEventFactory.workshopSessionStarted(workshopSession));

        this.outboxEventStore.save(events);

        log.info("Started workshop session for customer {} and published events {}", customerId, events);

        return workshopSession.getId();
    }

    @Command
    public void closeWorkshopSession(@NotNull @Valid CloseWorkshopSessionCommand command) {
        WorkshopSession workshopSession = this.workshopSessionRepository.findById(command.id().getValue()) //
                .orElseThrow(() -> ResourceNotFoundException.workshopSession(command.id()));

        List<SchaffbarEvent> events = workshopSession.close(command);

        this.outboxEventStore.save(events);

        log.info("Closed workshop session with id {} (workshop {}, tools {}) and published events {}", command.id(), command.workshopTotal(),
                command.toolTotal(), events);
    }

    // ------------------------------------------------------------------------
    // helper

    // WS-1 – WorkshopUsageStart starts a session only without an open one, so an open one here is a bug
    private void ensureNoOpenWorkshopSession(CustomerId customerId) {
        if (this.workshopSessionRepository.findOpenWorkshopSession(customerId).isPresent()) {
            throw new IllegalStateException("Customer [id: " + customerId.getValue() + "] already has an open workshop session");
        }
    }

}
