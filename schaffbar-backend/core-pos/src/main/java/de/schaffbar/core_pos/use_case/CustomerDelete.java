package de.schaffbar.core_pos.use_case;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.customer.CustomerService;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentService;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentStatus;
import de.schaffbar.core_pos.domain.tool.ToolService;
import de.schaffbar.core_pos.domain.tool.ToolViews.ToolView;
import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationService;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionService;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionViews.WorkshopSessionView;
import de.schaffbar.core_pos.shared.cqrs.Command;
import de.schaffbar.core_pos.shared.exception.CustomerHasOpenWorkshopSessionException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.CustomerId;

@Service
@Validated
@RequiredArgsConstructor
public class CustomerDelete {

    private final @NonNull CustomerService customerService;

    private final @NonNull WorkshopSessionService workshopSessionService;

    private final @NonNull RfidTagAssignmentService rfidTagAssignmentService;

    private final @NonNull ToolCertificationService toolCertificationService;

    private final @NonNull ToolService toolService;

    @Command
    public void process(@NotNull @Valid CustomerId customerId) {
        this.customerService.getCustomer(customerId) //
                .orElseThrow(() -> ResourceNotFoundException.customer(customerId));

        ensureNoOpenWorkshopSession(customerId);

        // C-4, C-9 – what only belongs to the customer goes with them; paid sessions, usages and the certifications
        // they gave to others stay (C-11)
        releaseRfidTag(customerId);
        this.toolCertificationService.deleteToolCertifications(customerId);
        removeAsInstructor(customerId);

        this.customerService.deleteCustomer(customerId);
    }

    // ------------------------------------------------------------------------
    // helper

    // the session still has to be closed and paid (C-5)
    private void ensureNoOpenWorkshopSession(CustomerId customerId) {
        this.workshopSessionService.getOpenWorkshopSession(customerId) //
                .ifPresent(this::throwCustomerHasOpenWorkshopSessionException);
    }

    private void releaseRfidTag(CustomerId customerId) {
        this.rfidTagAssignmentService.getRfidTagAssignment(customerId) //
                .ifPresent(assignment -> {
                    if (assignment.status() == RfidTagAssignmentStatus.ASSIGNED) {
                        this.rfidTagAssignmentService.unassignRfidTag(customerId);
                    }
                    else {
                        this.rfidTagAssignmentService.cancelRfidTagAssignment(customerId);
                    }
                });
    }

    private void removeAsInstructor(CustomerId customerId) {
        this.toolService.getToolsOfInstructor(customerId).stream() //
                .map(ToolView::id) //
                .forEach(toolId -> this.toolService.removeInstructors(toolId, List.of(customerId)));
    }

    private void throwCustomerHasOpenWorkshopSessionException(WorkshopSessionView openSession) {
        throw new CustomerHasOpenWorkshopSessionException(openSession.customerId());
    }

}
