package de.schaffbar.core_pos.read_facade;

import static de.schaffbar.core_pos.builder.ViewBuilder.customerView;
import static de.schaffbar.core_pos.builder.ViewBuilder.rfidTagAssignmentView;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.schaffbar.core_pos.domain.customer.CustomerService;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentService;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentViews.RfidTagAssignmentView;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException.Resource;
import de.schaffbar.core_pos.shared.id.CustomerId;

@ExtendWith(MockitoExtension.class)
class RfidTagAssignmentReadFacadeTest {

    @Mock
    private RfidTagAssignmentService rfidTagAssignmentService;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private RfidTagAssignmentReadFacade rfidTagAssignmentReadFacade;

    // ------------------------------------------------------------------------
    // getRfidTagAssignment(CustomerId)

    @Test
    void getRfidTagAssignment_ofACustomerWithTag_returnsTheAssignment() {
        // given
        RfidTagAssignmentView assignment = givenCustomerWithAssignment();

        // when
        Optional<RfidTagAssignmentView> result = this.rfidTagAssignmentReadFacade.getRfidTagAssignment(assignment.customerId());

        // then
        assertThat(result).contains(assignment);
    }

    @Test
    void getRfidTagAssignment_ofACustomerWithoutTag_isEmpty() {
        // given
        CustomerId customerId = givenCustomerWithoutAssignment();

        // when
        Optional<RfidTagAssignmentView> assignment = this.rfidTagAssignmentReadFacade.getRfidTagAssignment(customerId);

        // then
        assertThat(assignment).isEmpty();
    }

    @Test
    void getRfidTagAssignment_ofAnUnknownCustomer_isRejected() {
        // given
        CustomerId unknown = givenUnknownCustomer();

        // when
        Throwable thrown = catchThrowable(() -> this.rfidTagAssignmentReadFacade.getRfidTagAssignment(unknown));

        // then
        assertThat(thrown) //
                .isInstanceOf(ResourceNotFoundException.class) //
                .hasFieldOrPropertyWithValue("resource", Resource.CUSTOMER);
        expectNoAssignmentLookedUp();
    }

    // ------------------------------------------------------------------------
    // given

    private RfidTagAssignmentView givenCustomerWithAssignment() {
        RfidTagAssignmentView assignment = rfidTagAssignmentView().build();
        when(this.customerService.getCustomer(assignment.customerId())).thenReturn(Optional.of(customerView(assignment.customerId()).build()));
        when(this.rfidTagAssignmentService.getRfidTagAssignment(assignment.customerId())).thenReturn(Optional.of(assignment));

        return assignment;
    }

    private CustomerId givenCustomerWithoutAssignment() {
        CustomerId customerId = CustomerId.random();
        when(this.customerService.getCustomer(customerId)).thenReturn(Optional.of(customerView(customerId).build()));
        when(this.rfidTagAssignmentService.getRfidTagAssignment(customerId)).thenReturn(Optional.empty());

        return customerId;
    }

    private CustomerId givenUnknownCustomer() {
        CustomerId unknown = CustomerId.random();
        when(this.customerService.getCustomer(unknown)).thenReturn(Optional.empty());

        return unknown;
    }

    // ------------------------------------------------------------------------
    // then

    private void expectNoAssignmentLookedUp() {
        verifyNoInteractions(this.rfidTagAssignmentService);
    }

}
