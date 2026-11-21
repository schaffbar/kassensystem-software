package de.schaffbar.core_pos.read_facade;

import static java.util.Comparator.comparing;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.customer.CustomerService;
import de.schaffbar.core_pos.domain.customer.CustomerViews.CustomerView;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderService;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderViews.RfidReaderView;
import de.schaffbar.core_pos.domain.rfid_tag.RfidTagService;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentService;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentViews.RfidTagAssignmentView;
import de.schaffbar.core_pos.domain.tool.ToolService;
import de.schaffbar.core_pos.domain.tool.ToolViews.ToolView;
import de.schaffbar.core_pos.domain.tool_usage.ToolUsageService;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionService;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionViews.WorkshopSessionView;
import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageService;
import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageViews.WorkshopUsageView;
import de.schaffbar.core_pos.read_facade.DeviceViews.WorkshopStayView;
import de.schaffbar.core_pos.shared.billing.BillableDuration;
import de.schaffbar.core_pos.shared.cqrs.Query;
import de.schaffbar.core_pos.shared.exception.NoCustomerAssignedException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.MacAddress;
import de.schaffbar.core_pos.shared.id.RfidReaderId;
import de.schaffbar.core_pos.shared.id.RfidTagId;
import de.schaffbar.core_pos.shared.id.ToolId;

@Service
@Validated
@RequiredArgsConstructor
public class DeviceReadFacade {

    private final @NonNull RfidReaderService rfidReaderService;

    private final @NonNull RfidTagService rfidTagService;

    private final @NonNull RfidTagAssignmentService rfidTagAssignmentService;

    private final @NonNull CustomerService customerService;

    private final @NonNull ToolService toolService;

    private final @NonNull ToolUsageService toolUsageService;

    private final @NonNull WorkshopSessionService workshopSessionService;

    private final @NonNull WorkshopUsageService workshopUsageService;

    @Query
    public boolean isRfidReaderRegistered(@NotNull @Valid MacAddress macAddress) {
        return this.rfidReaderService.getRfidReader(macAddress).isPresent();
    }

    @Query
    public RfidReaderView getRfidReader(@NotNull @Valid MacAddress macAddress) {
        return this.rfidReaderService.getRfidReader(macAddress) //
                .orElseThrow(() -> ResourceNotFoundException.rfidReader(macAddress));
    }

    // the person a tag held to a reader belongs to
    @Query
    public CustomerView getCustomer(@NotNull @Valid RfidTagId rfidTagId) {
        this.rfidTagService.getRfidTag(rfidTagId) //
                .orElseThrow(() -> ResourceNotFoundException.rfidTag(rfidTagId));

        CustomerId customerId = this.rfidTagAssignmentService.getRfidTagAssignment(rfidTagId) //
                .map(RfidTagAssignmentView::customerId) //
                .orElseThrow(() -> new NoCustomerAssignedException(rfidTagId));

        return this.customerService.getCustomer(customerId) //
                .orElseThrow(() -> ResourceNotFoundException.customer(customerId));
    }

    @Query
    public boolean isRfidTagAssigned(@NotNull @Valid RfidTagId rfidTagId) {
        return this.rfidTagAssignmentService.getRfidTagAssignment(rfidTagId).isPresent();
    }

    @Query
    public Optional<ToolView> getTool(@NotNull @Valid RfidReaderId switchBoxId) {
        return this.toolService.getTool(switchBoxId);
    }

    @Query
    public boolean isToolInUse(@NotNull @Valid CustomerId customerId, @NotNull @Valid ToolId toolId) {
        return this.toolUsageService.getActiveToolUsage(customerId, toolId).isPresent();
    }

    // read right after a check-in or check-out, so the session is open
    @Query
    public WorkshopStayView getWorkshopStay(@NotNull @Valid CustomerId customerId) {
        WorkshopSessionView session = this.workshopSessionService.getOpenWorkshopSession(customerId) //
                .orElseThrow(() -> new IllegalStateException("Customer [id: " + customerId.getValue() + "] has no open workshop session"));

        List<WorkshopUsageView> usages = this.workshopUsageService.getWorkshopUsages(session.id());
        WorkshopUsageView last = usages.stream() //
                .max(comparing(WorkshopUsageView::entryTime)) //
                .orElseThrow(() -> new IllegalStateException("Workshop session [id: " + session.id().getValue() + "] has no usage"));

        var finishedUnits = usages.stream() //
                .map(WorkshopUsageView::billableDuration) //
                .filter(Objects::nonNull) //
                .mapToLong(BillableDuration::getUnits) //
                .sum();

        return WorkshopStayView.builder() //
                .finishedUnits(finishedUnits) //
                .entryTime(last.entryTime()) //
                .exitTime(last.exitTime()) //
                .build();
    }

}
