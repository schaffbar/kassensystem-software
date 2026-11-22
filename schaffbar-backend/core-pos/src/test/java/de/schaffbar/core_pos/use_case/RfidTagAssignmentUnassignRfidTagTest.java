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
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException.Resource;
import de.schaffbar.core_pos.shared.id.CustomerId;

@ExtendWith(MockitoExtension.class)
class RfidTagAssignmentUnassignRfidTagTest {

    @Mock
    private CustomerService customerService;

    @Mock
    private RfidTagAssignmentService rfidTagAssignmentService;

    @InjectMocks
    private RfidTagAssignmentUnassignRfidTag rfidTagAssignmentUnassignRfidTag;

    @Test
    void unassignsTheTagOfAnExistingCustomer() {
        // given
        CustomerId customerId = givenCustomer();

        // when
        this.rfidTagAssignmentUnassignRfidTag.process(customerId);

        // then
        expectUnassigned(customerId);
    }

    @Test
    void rejectsAnUnknownCustomer() {
        // given
        CustomerId unknown = givenUnknownCustomer();

        // when
        Throwable thrown = catchThrowable(() -> this.rfidTagAssignmentUnassignRfidTag.process(unknown));

        // then
        assertThat(thrown) //
                .isInstanceOf(ResourceNotFoundException.class) //
                .hasFieldOrPropertyWithValue("resource", Resource.CUSTOMER);
        expectNothingUnassigned();
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

    // ------------------------------------------------------------------------
    // then

    private void expectUnassigned(CustomerId customerId) {
        verify(this.rfidTagAssignmentService).unassignRfidTag(customerId);
    }

    private void expectNothingUnassigned() {
        verifyNoInteractions(this.rfidTagAssignmentService);
    }

}
