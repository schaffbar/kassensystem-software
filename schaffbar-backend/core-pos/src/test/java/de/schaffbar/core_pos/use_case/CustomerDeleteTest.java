package de.schaffbar.core_pos.use_case;

import static de.schaffbar.core_pos.builder.ViewBuilder.customerView;
import static de.schaffbar.core_pos.builder.ViewBuilder.openWorkshopSessionView;
import static de.schaffbar.core_pos.builder.ViewBuilder.rfidTagAssignmentView;
import static de.schaffbar.core_pos.builder.ViewBuilder.toolView;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
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

import de.schaffbar.core_pos.domain.customer.CustomerService;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentService;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentStatus;
import de.schaffbar.core_pos.domain.tool.ToolService;
import de.schaffbar.core_pos.domain.tool.ToolViews.ToolView;
import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationService;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionService;
import de.schaffbar.core_pos.shared.exception.CustomerHasOpenWorkshopSessionException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException.Resource;
import de.schaffbar.core_pos.shared.id.CustomerId;

@ExtendWith(MockitoExtension.class)
class CustomerDeleteTest {

    @Mock
    private CustomerService customerService;

    @Mock
    private WorkshopSessionService workshopSessionService;

    @Mock
    private RfidTagAssignmentService rfidTagAssignmentService;

    @Mock
    private ToolCertificationService toolCertificationService;

    @Mock
    private ToolService toolService;

    @InjectMocks
    private CustomerDelete customerDelete;

    @Test
    void deletesTheCertificationsBeforeTheCustomer() {
        // given
        CustomerId customerId = givenCustomerWithoutOpenSession();
        givenNoRfidTagAssignment(customerId);
        givenToolsOfInstructor(customerId);

        // when
        this.customerDelete.process(customerId);

        // then
        expectCertificationsDeletedBeforeTheCustomer(customerId);
        expectRfidTagNotTouched();
    }

    @Test
    void unassignsTheTagBeforeDeletingTheCustomer() {
        // given
        CustomerId customerId = givenCustomerWithoutOpenSession();
        givenRfidTagAssignment(customerId, RfidTagAssignmentStatus.ASSIGNED);
        givenToolsOfInstructor(customerId);

        // when
        this.customerDelete.process(customerId);

        // then
        expectRfidTagUnassignedBeforeTheCustomerIsDeleted(customerId);
    }

    @Test
    void cancelsAWaitingRequestBeforeDeletingTheCustomer() {
        // given
        CustomerId customerId = givenCustomerWithoutOpenSession();
        givenRfidTagAssignment(customerId, RfidTagAssignmentStatus.WAITING_FOR_ASSIGNMENT);
        givenToolsOfInstructor(customerId);

        // when
        this.customerDelete.process(customerId);

        // then
        expectRequestCancelledBeforeTheCustomerIsDeleted(customerId);
    }

    @Test
    void removesTheCustomerAsInstructorOfTheirTools() {
        // given
        CustomerId customerId = givenCustomerWithoutOpenSession();
        givenNoRfidTagAssignment(customerId);
        ToolView tool = toolView().instructors(Set.of(customerId, CustomerId.random())).build();
        ToolView otherTool = toolView().instructors(Set.of(customerId)).build();
        givenToolsOfInstructor(customerId, tool, otherTool);

        // when
        this.customerDelete.process(customerId);

        // then
        expectRemovedAsInstructorBeforeTheCustomerIsDeleted(customerId, tool, otherTool);
    }

    @Test
    void rejectsACustomerWithAnOpenSession() {
        // given
        CustomerId customerId = givenCustomerWithOpenSession();

        // when
        Throwable thrown = catchThrowable(() -> this.customerDelete.process(customerId));

        // then
        assertThat(thrown) //
                .isInstanceOf(CustomerHasOpenWorkshopSessionException.class) //
                .hasFieldOrPropertyWithValue("code", CustomerHasOpenWorkshopSessionException.CODE);
        expectNothingDeleted();
    }

    @Test
    void rejectsAnUnknownCustomer() {
        // given
        CustomerId unknown = givenUnknownCustomer();

        // when
        Throwable thrown = catchThrowable(() -> this.customerDelete.process(unknown));

        // then
        assertThat(thrown) //
                .isInstanceOf(ResourceNotFoundException.class) //
                .hasFieldOrPropertyWithValue("resource", Resource.CUSTOMER);
        expectNothingDeleted();
    }

    // ------------------------------------------------------------------------
    // given

    private CustomerId givenCustomer() {
        CustomerId customerId = CustomerId.random();
        when(this.customerService.getCustomer(customerId)).thenReturn(Optional.of(customerView(customerId).build()));

        return customerId;
    }

    private CustomerId givenCustomerWithoutOpenSession() {
        CustomerId customerId = givenCustomer();
        when(this.workshopSessionService.getOpenWorkshopSession(customerId)).thenReturn(Optional.empty());

        return customerId;
    }

    private CustomerId givenCustomerWithOpenSession() {
        CustomerId customerId = givenCustomer();
        when(this.workshopSessionService.getOpenWorkshopSession(customerId)) //
                .thenReturn(Optional.of(openWorkshopSessionView().customerId(customerId).build()));

        return customerId;
    }

    private CustomerId givenUnknownCustomer() {
        CustomerId unknown = CustomerId.random();
        when(this.customerService.getCustomer(unknown)).thenReturn(Optional.empty());

        return unknown;
    }

    private void givenNoRfidTagAssignment(CustomerId customerId) {
        when(this.rfidTagAssignmentService.getRfidTagAssignment(customerId)).thenReturn(Optional.empty());
    }

    private void givenRfidTagAssignment(CustomerId customerId, RfidTagAssignmentStatus status) {
        when(this.rfidTagAssignmentService.getRfidTagAssignment(customerId)) //
                .thenReturn(Optional.of(rfidTagAssignmentView().customerId(customerId).status(status).build()));
    }

    private void givenToolsOfInstructor(CustomerId customerId, ToolView... tools) {
        when(this.toolService.getToolsOfInstructor(customerId)).thenReturn(List.of(tools));
    }

    // ------------------------------------------------------------------------
    // then

    private void expectCertificationsDeletedBeforeTheCustomer(CustomerId customerId) {
        InOrder order = inOrder(this.toolCertificationService, this.customerService);
        order.verify(this.toolCertificationService).deleteToolCertifications(customerId);
        order.verify(this.customerService).deleteCustomer(customerId);
    }

    private void expectRfidTagUnassignedBeforeTheCustomerIsDeleted(CustomerId customerId) {
        InOrder order = inOrder(this.rfidTagAssignmentService, this.customerService);
        order.verify(this.rfidTagAssignmentService).unassignRfidTag(customerId);
        order.verify(this.customerService).deleteCustomer(customerId);
        verify(this.rfidTagAssignmentService, never()).cancelRfidTagAssignment(any());
    }

    private void expectRequestCancelledBeforeTheCustomerIsDeleted(CustomerId customerId) {
        InOrder order = inOrder(this.rfidTagAssignmentService, this.customerService);
        order.verify(this.rfidTagAssignmentService).cancelRfidTagAssignment(customerId);
        order.verify(this.customerService).deleteCustomer(customerId);
        verify(this.rfidTagAssignmentService, never()).unassignRfidTag(any());
    }

    private void expectRemovedAsInstructorBeforeTheCustomerIsDeleted(CustomerId customerId, ToolView... tools) {
        InOrder order = inOrder(this.toolService, this.customerService);
        for (ToolView tool : tools) {
            order.verify(this.toolService).removeInstructors(tool.id(), List.of(customerId));
        }
        order.verify(this.customerService).deleteCustomer(customerId);
    }

    private void expectRfidTagNotTouched() {
        verify(this.rfidTagAssignmentService, never()).unassignRfidTag(any());
        verify(this.rfidTagAssignmentService, never()).cancelRfidTagAssignment(any());
    }

    private void expectNothingDeleted() {
        verifyNoInteractions(this.rfidTagAssignmentService, this.toolCertificationService, this.toolService);
        verify(this.customerService, never()).deleteCustomer(any());
    }

}
