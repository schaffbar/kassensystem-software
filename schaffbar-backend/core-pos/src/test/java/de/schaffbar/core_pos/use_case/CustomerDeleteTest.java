package de.schaffbar.core_pos.use_case;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.schaffbar.core_pos.builder.ViewBuilder;
import de.schaffbar.core_pos.domain.customer.CustomerService;
import de.schaffbar.core_pos.domain.customer.CustomerViews.CustomerView;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentService;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentStatus;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentType;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentViews.RfidTagAssignmentView;
import de.schaffbar.core_pos.domain.rfid_tag_assignment_history.RfidTagAssignmentHistoryService;
import de.schaffbar.core_pos.domain.tool.ToolService;
import de.schaffbar.core_pos.domain.tool.ToolViews.ToolView;
import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationService;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionService;
import de.schaffbar.core_pos.shared.exception.CustomerHasOpenWorkshopSessionException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.RfidTagAssignmentId;
import de.schaffbar.core_pos.shared.id.ToolId;

@ExtendWith(MockitoExtension.class)
class CustomerDeleteTest {

    private final CustomerId customerId = CustomerId.random();

    @Mock
    private CustomerService customerService;

    @Mock
    private WorkshopSessionService workshopSessionService;

    @Mock
    private RfidTagAssignmentService rfidTagAssignmentService;

    @Mock
    private RfidTagAssignmentHistoryService rfidTagAssignmentHistoryService;

    @Mock
    private ToolCertificationService toolCertificationService;

    @Mock
    private ToolService toolService;

    @InjectMocks
    private CustomerDelete customerDelete;

    @Test
    void deletesCustomerWithoutTagAndCertifications() {
        noOpenSession();
        when(this.rfidTagAssignmentService.getRfidTagAssignment(this.customerId)).thenReturn(Optional.empty());
        when(this.toolService.getTools()).thenReturn(List.of());

        this.customerDelete.process(this.customerId);

        verify(this.toolCertificationService).deleteAllByCustomerId(this.customerId);
        verify(this.customerService).deleteCustomer(this.customerId);
        verifyNoInteractions(this.rfidTagAssignmentHistoryService);
    }

    @Test
    void rejectsCustomerWithOpenSession() {
        customerExists();
        when(this.workshopSessionService.getOpenWorkshopSession(this.customerId)) //
                .thenReturn(Optional.of(ViewBuilder.openWorkshopSessionView().customerId(this.customerId).build()));

        assertThatThrownBy(() -> this.customerDelete.process(this.customerId)) //
                .isInstanceOf(CustomerHasOpenWorkshopSessionException.class);

        verifyNoInteractions(this.rfidTagAssignmentService, this.toolCertificationService);
        verify(this.customerService, never()).deleteCustomer(any());
    }

    @Test
    void rejectsUnknownCustomer() {
        CustomerId unknownId = CustomerId.random();
        when(this.customerService.getCustomer(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.customerDelete.process(unknownId)) //
                .isInstanceOf(ResourceNotFoundException.class);

        verify(this.customerService, never()).deleteCustomer(any());
    }

    @Test
    void freesAssignedTagBeforeDeletingCustomer() {
        noOpenSession();
        RfidTagAssignmentView assignment = assignment(RfidTagAssignmentStatus.ASSIGNED);
        when(this.rfidTagAssignmentService.getRfidTagAssignment(this.customerId)).thenReturn(Optional.of(assignment));
        when(this.toolService.getTools()).thenReturn(List.of());

        this.customerDelete.process(this.customerId);

        InOrder order = inOrder(this.rfidTagAssignmentHistoryService, this.rfidTagAssignmentService, this.customerService);
        order.verify(this.rfidTagAssignmentHistoryService).moveToHistory(assignment);
        order.verify(this.rfidTagAssignmentService).unassignRfidTag(this.customerId);
        order.verify(this.customerService).deleteCustomer(this.customerId);
        verify(this.rfidTagAssignmentService, never()).cancelRfidTagAssignmentRequest(any());
    }

    @Test
    void cancelsWaitingTagRequest() {
        noOpenSession();
        when(this.rfidTagAssignmentService.getRfidTagAssignment(this.customerId)) //
                .thenReturn(Optional.of(assignment(RfidTagAssignmentStatus.WAITING_FOR_ASSIGNMENT)));
        when(this.toolService.getTools()).thenReturn(List.of());

        this.customerDelete.process(this.customerId);

        verify(this.rfidTagAssignmentService).cancelRfidTagAssignmentRequest(this.customerId);
        verify(this.rfidTagAssignmentService, never()).unassignRfidTag(any());
        verifyNoInteractions(this.rfidTagAssignmentHistoryService);
        verify(this.customerService).deleteCustomer(this.customerId);
    }

    @Test
    void removesCustomerAsInstructorOfTheirToolsOnly() {
        noOpenSession();
        when(this.rfidTagAssignmentService.getRfidTagAssignment(this.customerId)).thenReturn(Optional.empty());
        ToolView instructedTool = tool(Set.of(this.customerId, CustomerId.random()));
        ToolView otherTool = tool(Set.of(CustomerId.random()));
        when(this.toolService.getTools()).thenReturn(List.of(instructedTool, otherTool));

        this.customerDelete.process(this.customerId);

        verify(this.toolService).removeInstructors(instructedTool.id(), List.of(this.customerId));
        verify(this.toolService, never()).removeInstructors(otherTool.id(), List.of(this.customerId));
        verify(this.customerService).deleteCustomer(this.customerId);
    }

    // ------------------------------------------------------------------------
    // helper

    private void customerExists() {
        when(this.customerService.getCustomer(this.customerId)).thenReturn(Optional.of(mock(CustomerView.class)));
    }

    private void noOpenSession() {
        customerExists();
        when(this.workshopSessionService.getOpenWorkshopSession(this.customerId)).thenReturn(Optional.empty());
    }

    private RfidTagAssignmentView assignment(RfidTagAssignmentStatus status) {
        return RfidTagAssignmentView.builder() //
                .id(RfidTagAssignmentId.random()) //
                .customerId(this.customerId) //
                .assignmentType(RfidTagAssignmentType.FIXED) //
                .status(status) //
                .build();
    }

    private static ToolView tool(Set<CustomerId> instructors) {
        return ToolView.builder() //
                .id(ToolId.random()) //
                .name("Bandsäge") //
                .instructors(instructors) //
                .build();
    }

}
