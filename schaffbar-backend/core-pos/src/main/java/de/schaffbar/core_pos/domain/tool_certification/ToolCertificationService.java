package de.schaffbar.core_pos.domain.tool_certification;

import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationCommands.CreateToolCertificationCommand;
import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationViews.ToolCertificationView;
import de.schaffbar.core_pos.shared.cqrs.Command;
import de.schaffbar.core_pos.shared.cqrs.Query;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.event.outbox.OutboxEventStore;
import de.schaffbar.core_pos.shared.exception.CertificationAlreadyExistsException;
import de.schaffbar.core_pos.shared.exception.CertificationPermanentlyRevokedException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolCertificationId;
import de.schaffbar.core_pos.shared.id.ToolId;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class ToolCertificationService {

    private final @NonNull ToolCertificationRepository toolCertificationRepository;

    private final @NonNull OutboxEventStore outboxEventStore;

    // ------------------------------------------------------------------------
    // query

    @Query
    public List<ToolCertificationView> getToolCertifications(@NotNull @Valid CustomerId customerId) {
        return this.toolCertificationRepository.findByCustomerId(customerId).stream() //
                .map(ToolCertificationViews.MAPPER::toToolCertificationView) //
                .toList();
    }

    @Query
    public List<ToolCertificationView> getToolCertifications(@NotNull @Valid ToolId toolId) {
        return this.toolCertificationRepository.findByToolId(toolId).stream() //
                .map(ToolCertificationViews.MAPPER::toToolCertificationView) //
                .toList();
    }

    @Query
    public Optional<ToolCertificationView> getToolCertification(@NotNull @Valid ToolCertificationId id) {
        return this.toolCertificationRepository.findById(id.getValue()) //
                .map(ToolCertificationViews.MAPPER::toToolCertificationView);
    }

    @Query
    public boolean hasActiveToolCertification(@NotNull @Valid CustomerId customerId, @NotNull @Valid ToolId toolId) {
        return this.toolCertificationRepository.findByCustomerIdAndToolId(customerId, toolId) //
                .filter(certification -> certification.getStatus() == ToolCertificationStatus.ACTIVE) //
                .isPresent();
    }

    // ------------------------------------------------------------------------
    // command

    @Command
    public void createToolCertification(@NotNull @Valid CreateToolCertificationCommand command) {
        ensureNoCertificationExists(command.customerId(), command.toolId());

        ToolCertification certification = ToolCertification.of(command);
        this.toolCertificationRepository.save(certification);

        List<SchaffbarEvent> events = List.of(ToolCertificationEventFactory.toolCertificationCreated(certification));
        this.outboxEventStore.save(events);

        log.info("Created tool certification for customer {} on tool {} and published events {}", command.customerId(), command.toolId(), events);
    }

    @Command
    public void pauseToolCertification(@NotNull @Valid CustomerId customerId, @NotNull @Valid ToolId toolId) {
        ToolCertification certification = findOrThrow(customerId, toolId);

        List<SchaffbarEvent> events = certification.pause();
        this.outboxEventStore.save(events);

        log.info("Paused tool certification for customer {} on tool {} and published events {}", customerId, toolId, events);
    }

    @Command
    public void reactivateToolCertification(@NotNull @Valid CustomerId customerId, @NotNull @Valid ToolId toolId) {
        ToolCertification certification = findOrThrow(customerId, toolId);

        List<SchaffbarEvent> events = certification.reactivate();
        this.outboxEventStore.save(events);

        log.info("Reactivated tool certification for customer {} on tool {} and published events {}", customerId, toolId, events);
    }

    @Command
    public void revokeToolCertification(@NotNull @Valid CustomerId customerId, @NotNull @Valid ToolId toolId) {
        ToolCertification certification = findOrThrow(customerId, toolId);

        List<SchaffbarEvent> events = certification.revoke();
        this.outboxEventStore.save(events);

        log.info("Revoked tool certification for customer {} on tool {} and published events {}", customerId, toolId, events);
    }

    @Command
    public void deleteToolCertification(@NotNull @Valid CustomerId customerId, @NotNull @Valid ToolId toolId) {
        ToolCertification certification = findOrThrow(customerId, toolId);

        this.toolCertificationRepository.delete(certification);
        List<SchaffbarEvent> events = List.of(ToolCertificationEventFactory.toolCertificationDeleted(certification));

        this.outboxEventStore.save(events);

        log.info("Deleted tool certification for customer {} on tool {} and published events {}", customerId, toolId, events);
    }

    @Command
    public void deleteToolCertifications(@NotNull @Valid ToolId toolId) {
        List<ToolCertification> certifications = this.toolCertificationRepository.findByToolId(toolId);
        if (certifications.isEmpty()) {
            return;
        }

        this.toolCertificationRepository.deleteAll(certifications);
        List<SchaffbarEvent> events = certifications.stream() //
                .map(ToolCertificationEventFactory::toolCertificationDeleted) //
                .toList();

        this.outboxEventStore.save(events);

        log.info("Deleted {} tool certifications for tool {} and published events {}", certifications.size(), toolId, events);
    }

    @Command
    public void deleteToolCertifications(@NotNull @Valid CustomerId customerId) {
        List<ToolCertification> certifications = this.toolCertificationRepository.findByCustomerId(customerId);
        if (certifications.isEmpty()) {
            return;
        }

        this.toolCertificationRepository.deleteAll(certifications);
        List<SchaffbarEvent> events = certifications.stream() //
                .map(ToolCertificationEventFactory::toolCertificationDeleted) //
                .toList();

        this.outboxEventStore.save(events);

        log.info("Deleted {} tool certifications of customer {} and published events {}", certifications.size(), customerId, events);
    }

    // ------------------------------------------------------------------------
    // helper

    private ToolCertification findOrThrow(CustomerId customerId, ToolId toolId) {
        return this.toolCertificationRepository.findByCustomerIdAndToolId(customerId, toolId) //
                .orElseThrow(() -> ResourceNotFoundException.toolCertification(customerId, toolId));
    }

    // a revoked certification stays and blocks a new one (TC-2)
    private void ensureNoCertificationExists(CustomerId customerId, ToolId toolId) {
        this.toolCertificationRepository.findByCustomerIdAndToolId(customerId, toolId) //
                .ifPresent(existing -> {
                    if (existing.getStatus() == ToolCertificationStatus.REVOKED) {
                        throw new CertificationPermanentlyRevokedException(customerId, toolId);
                    }
                    throw new CertificationAlreadyExistsException(customerId, toolId);
                });
    }

}
