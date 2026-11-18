package de.schaffbar.core_pos.domain.tool;

import static org.apache.commons.lang3.BooleanUtils.isFalse;

import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.tool.ToolCommands.CreateToolCommand;
import de.schaffbar.core_pos.domain.tool.ToolCommands.SetWlanRelaisCommand;
import de.schaffbar.core_pos.domain.tool.ToolCommands.UpdateToolCommand;
import de.schaffbar.core_pos.domain.tool.ToolViews.ToolView;
import de.schaffbar.core_pos.shared.cqrs.Command;
import de.schaffbar.core_pos.shared.cqrs.Query;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.event.outbox.OutboxEventStore;
import de.schaffbar.core_pos.shared.exception.IpAddressAlreadyUsedException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.RfidReaderAlreadyAssignedToToolException;
import de.schaffbar.core_pos.shared.exception.ToolNameAlreadyUsedException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.RfidReaderId;
import de.schaffbar.core_pos.shared.id.ToolId;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class ToolService {

    private final @NonNull ToolRepository toolRepository;

    private final @NonNull OutboxEventStore outboxEventStore;

    // ------------------------------------------------------------------------
    // query

    @Query
    public List<ToolView> getTools() {
        return this.toolRepository.findAll().stream() //
                .map(ToolViews.MAPPER::toToolView) //
                .toList();
    }

    @Query
    public Optional<ToolView> getTool(@NotNull @Valid ToolId id) {
        return this.toolRepository.findById(id.getValue()) //
                .map(ToolViews.MAPPER::toToolView);
    }

    @Query
    public Optional<ToolView> getTool(@NotNull @Valid RfidReaderId rfidReaderId) {
        return this.toolRepository.findByRfidReaderId(rfidReaderId) //
                .map(ToolViews.MAPPER::toToolView);
    }

    @Query
    public List<ToolView> getToolsOfInstructor(@NotNull @Valid CustomerId instructorId) {
        return this.toolRepository.findByInstructorIdsContaining(instructorId).stream() //
                .map(ToolViews.MAPPER::toToolView) //
                .toList();
    }

    // ------------------------------------------------------------------------
    // command

    @Command
    public ToolId createTool(@NotNull @Valid CreateToolCommand command) {
        ensureNameUnused(command.name(), null);

        Tool tool = Tool.of(command);
        Tool savedTool = this.toolRepository.save(tool);
        List<SchaffbarEvent> events = List.of(ToolEventFactory.toolCreated(savedTool));

        this.outboxEventStore.save(events);

        log.info("Created tool with id {} and published events {}", savedTool.getId(), events);

        return savedTool.getId();
    }

    @Command
    public void updateTool(@NotNull @Valid UpdateToolCommand command) {
        Tool tool = findOrThrow(command.id());
        ensureNameUnused(command.name(), command.id());

        List<SchaffbarEvent> events = tool.update(command);
        this.outboxEventStore.save(events);

        log.info("Updated tool with id {} and published events {}", command.id(), events);
    }

    @Command
    public void setWlanRelais(@NotNull @Valid SetWlanRelaisCommand command) {
        Tool tool = findOrThrow(command.id());

        this.toolRepository.findByIpAddress(command.ipAddress()) //
                .filter(existing -> isFalse(command.id().sameValueAs(existing.getId()))) //
                .ifPresent(existing -> {
                    throw new IpAddressAlreadyUsedException(command.ipAddress(), existing.getId());
                });

        List<SchaffbarEvent> events = tool.setWlanRelais(command);
        this.outboxEventStore.save(events);

        log.info("Set WLAN relais of tool with id {} and published events {}", command.id(), events);
    }

    @Command
    public void clearWlanRelais(@NotNull @Valid ToolId toolId) {
        Tool tool = findOrThrow(toolId);

        List<SchaffbarEvent> events = tool.clearWlanRelais();
        this.outboxEventStore.save(events);

        log.info("Cleared WLAN relais of tool with id {} and published events {}", toolId, events);
    }

    @Command
    public void assignRfidReader(@NotNull @Valid ToolId toolId, @NotNull @Valid RfidReaderId rfidReaderId) {
        Tool tool = findOrThrow(toolId);

        this.toolRepository.findByRfidReaderId(rfidReaderId) //
                .filter(existing -> isFalse(toolId.sameValueAs(existing.getId()))) //
                .ifPresent(existing -> {
                    throw new RfidReaderAlreadyAssignedToToolException(rfidReaderId, existing.getId());
                });

        List<SchaffbarEvent> events = tool.assignRfidReader(rfidReaderId);
        this.outboxEventStore.save(events);

        log.info("Assigned RFID reader {} to tool {} and published events {}", rfidReaderId, toolId, events);
    }

    @Command
    public void clearRfidReader(@NotNull @Valid ToolId toolId) {
        Tool tool = findOrThrow(toolId);

        List<SchaffbarEvent> events = tool.clearRfidReader();
        this.outboxEventStore.save(events);

        log.info("Cleared RFID reader from tool {} and published events {}", toolId, events);
    }

    @Command
    public void addInstructors(@NotNull @Valid ToolId toolId, @NotEmpty List<@NotNull @Valid CustomerId> instructorIds) {
        Tool tool = findOrThrow(toolId);

        List<SchaffbarEvent> events = tool.addInstructors(instructorIds);
        this.outboxEventStore.save(events);

        log.info("Added instructors {} to tool {} and published events {}", instructorIds, toolId, events);
    }

    @Command
    public void removeInstructors(@NotNull @Valid ToolId toolId, @NotEmpty List<@NotNull @Valid CustomerId> instructorIds) {
        Tool tool = findOrThrow(toolId);

        List<SchaffbarEvent> events = tool.removeInstructors(instructorIds);
        this.outboxEventStore.save(events);

        log.info("Removed instructors {} from tool {} and published events {}", instructorIds, toolId, events);
    }

    @Command
    public void deleteTool(@NotNull @Valid ToolId toolId) {
        Tool tool = findOrThrow(toolId);

        this.toolRepository.delete(tool);
        List<SchaffbarEvent> events = List.of(ToolEventFactory.toolDeleted(toolId));

        this.outboxEventStore.save(events);

        log.info("Deleted tool with id {} and published events {}", toolId, events);
    }

    // ------------------------------------------------------------------------
    // helper

    private Tool findOrThrow(ToolId toolId) {
        return this.toolRepository.findById(toolId.getValue()) //
                .orElseThrow(() -> ResourceNotFoundException.tool(toolId));
    }

    // names are unique without regard to case (T-2, T-15); toolId is the tool that keeps its name, null for a new one
    private void ensureNameUnused(String name, ToolId toolId) {
        this.toolRepository.findByNameIgnoreCase(name).stream() //
                .filter(existing -> isFalse(existing.getId().sameValueAs(toolId))) //
                .findFirst() //
                .ifPresent(existing -> {
                    throw new ToolNameAlreadyUsedException(name, existing.getId());
                });
    }

}
