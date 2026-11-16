package de.schaffbar.core_pos.use_case;

import static de.schaffbar.core_pos.builder.ViewBuilder.customerView;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;
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
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentType;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.CustomerId;

@ExtendWith(MockitoExtension.class)
class CustomerRequestRfidTagAssignmentTest {

    @Mock
    private CustomerService customerService;

    @Mock
    private RfidTagAssignmentService rfidTagAssignmentService;

    @InjectMocks
    private CustomerRequestRfidTagAssignment customerRequestRfidTagAssignment;

    @Test
    void requestsATagForAnExistingCustomer() {
        // given
        CustomerId customerId = givenCustomer();

        // when
        this.customerRequestRfidTagAssignment.process(customerId, RfidTagAssignmentType.TEMPORARY);

        // then
        verify(this.rfidTagAssignmentService).requestRfidTagAssignment(customerId, RfidTagAssignmentType.TEMPORARY);
    }

    @Test
    void rejectsAnUnknownCustomer() {
        // given
        CustomerId unknown = givenUnknownCustomer();

        // when
        Throwable thrown = catchThrowable(() -> this.customerRequestRfidTagAssignment.process(unknown, RfidTagAssignmentType.FIXED));

        // then
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(this.rfidTagAssignmentService);
    }

    // ------------------------------------------------------------------------
    // given

    private CustomerId givenCustomer() {
        CustomerId customerId = CustomerId.random();
        when(this.customerService.getCustomer(customerId)).thenReturn(Optional.of(customerView(customerId).build()));

        return customerId;
    }

    private CustomerId givenUnknownCustomer() {
        CustomerId unknown = CustomerId.random();
        when(this.customerService.getCustomer(unknown)).thenReturn(Optional.empty());

        return unknown;
    }

}
