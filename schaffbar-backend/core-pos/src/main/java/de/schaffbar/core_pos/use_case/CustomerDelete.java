package de.schaffbar.core_pos.use_case;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.customer.CustomerService;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentService;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentStatus;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentViews.RfidTagAssignmentView;
import de.schaffbar.core_pos.domain.rfid_tag_assignment_history.RfidTagAssignmentHistoryService;
import de.schaffbar.core_pos.domain.tool.ToolService;
import de.schaffbar.core_pos.domain.tool.ToolViews.ToolView;
import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationService;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionService;
import de.schaffbar.core_pos.shared.exception.CustomerHasOpenWorkshopSessionException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.CustomerId;

/**
 * Deletes a customer with everything that only belongs to them: the RFID tag becomes free again, their certifications
 * and their place as instructor are removed. Paid sessions and usages stay for the accounting, certifications the
 * customer has given to others stay as well.
 */
@Service
@Validated
@RequiredArgsConstructor
public class CustomerDelete {

    private final @NonNull CustomerService customerService;

    private final @NonNull WorkshopSessionService workshopSessionService;

    private final @NonNull RfidTagAssignmentService rfidTagAssignmentService;

    private final @NonNull RfidTagAssignmentHistoryService rfidTagAssignmentHistoryService;

    private final @NonNull ToolCertificationService toolCertificationService;

    private final @NonNull ToolService toolService;

    @Transactional
    public void process(@NotNull @Valid CustomerId customerId) {
        this.customerService.getCustomer(customerId) //
                .orElseThrow(() -> ResourceNotFoundException.customer(customerId));

        verifyNoOpenWorkshopSession(customerId);

        releaseRfidTag(customerId);
        this.toolCertificationService.deleteAllByCustomerId(customerId);
        removeAsInstructor(customerId);

        this.customerService.deleteCustomer(customerId);
    }

    // ------------------------------------------------------------------------
    // helper

    private void verifyNoOpenWorkshopSession(CustomerId customerId) {
        this.workshopSessionService.getOpenWorkshopSession(customerId) //
                .ifPresent(_ -> {
                    throw new CustomerHasOpenWorkshopSessionException(customerId);
                });
    }

    private void releaseRfidTag(CustomerId customerId) {
        this.rfidTagAssignmentService.getRfidTagAssignment(customerId) //
                .ifPresent(assignment -> {
                    if (assignment.status() == RfidTagAssignmentStatus.ASSIGNED) {
                        unassignRfidTag(assignment);
                    }
                    else {
                        this.rfidTagAssignmentService.cancelRfidTagAssignmentRequest(customerId);
                    }
                });
    }

    private void unassignRfidTag(RfidTagAssignmentView assignment) {
        this.rfidTagAssignmentHistoryService.moveToHistory(assignment);
        this.rfidTagAssignmentService.unassignRfidTag(assignment.customerId());
    }

    private void removeAsInstructor(CustomerId customerId) {
        this.toolService.getTools().stream() //
                .filter(tool -> tool.instructors().contains(customerId)) //
                .map(ToolView::id) //
                .forEach(toolId -> this.toolService.removeInstructors(toolId, List.of(customerId)));
    }

}
