package de.schaffbar.core_pos.use_case;

import static de.schaffbar.core_pos.builder.ViewBuilder.customerView;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.schaffbar.core_pos.domain.customer.CustomerService;
import de.schaffbar.core_pos.domain.tool.ToolService;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException.Resource;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;

@ExtendWith(MockitoExtension.class)
class ToolAddInstructorsTest {

    @Mock
    private ToolService toolService;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private ToolAddInstructors toolAddInstructors;

    @Test
    void addsThePeople() {
        // given
        ToolId toolId = ToolId.random();
        List<CustomerId> instructorIds = List.of(givenCustomer(), givenCustomer());

        // when
        this.toolAddInstructors.process(toolId, instructorIds);

        // then
        expectInstructorsAdded(toolId, instructorIds);
    }

    @Test
    void rejectsAnUnknownCustomer() {
        // given
        List<CustomerId> instructorIds = List.of(givenCustomer(), givenUnknownCustomer());

        // when
        Throwable thrown = catchThrowable(() -> this.toolAddInstructors.process(ToolId.random(), instructorIds));

        // then
assertThat(thrown) //
                .isInstanceOf(ResourceNotFoundException.class) //
                .hasFieldOrPropertyWithValue("resource", Resource.CUSTOMER);
        expectNoInstructorsAdded();
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

    private void expectInstructorsAdded(ToolId toolId, List<CustomerId> instructorIds) {
        verify(this.toolService).addInstructors(toolId, instructorIds);
    }

    private void expectNoInstructorsAdded() {
        verifyNoInteractions(this.toolService);
    }

}
