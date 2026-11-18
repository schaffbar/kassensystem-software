package de.schaffbar.core_pos.use_case;

import static de.schaffbar.core_pos.builder.ViewBuilder.customerView;
import static de.schaffbar.core_pos.builder.ViewBuilder.toolView;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.inOrder;
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
import de.schaffbar.core_pos.domain.tool.ToolService;
import de.schaffbar.core_pos.domain.tool.ToolViews.ToolView;
import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationCommands.CreateToolCertificationCommand;
import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationService;
import de.schaffbar.core_pos.shared.exception.CustomerIsInstructorException;
import de.schaffbar.core_pos.shared.exception.CustomerNotInstructorException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;

@ExtendWith(MockitoExtension.class)
class ToolCertificationCreateTest {

    @Mock
    private ToolCertificationService toolCertificationService;

    @Mock
    private CustomerService customerService;

    @Mock
    private ToolService toolService;

    @InjectMocks
    private ToolCertificationCreate toolCertificationCreate;

    @Test
    void certifiesEveryCustomer() {
        // given
        CustomerId instructorId = CustomerId.random();
        ToolView tool = givenToolWithInstructors(instructorId);
        CustomerId customerId = givenCustomer();
        CustomerId otherCustomerId = givenCustomer();

        // when
        this.toolCertificationCreate.process(List.of(customerId, otherCustomerId), tool.id(), instructorId);

        // then
        InOrder order = inOrder(this.toolCertificationService);
        order.verify(this.toolCertificationService).createToolCertification(new CreateToolCertificationCommand(customerId, tool.id(), instructorId));
        order.verify(this.toolCertificationService).createToolCertification(new CreateToolCertificationCommand(otherCustomerId, tool.id(), instructorId));
    }

    @Test
    void rejectsAnUnknownTool() {
        // given
        ToolId unknown = givenUnknownTool();

        // when
        Throwable thrown = catchThrowable(() -> this.toolCertificationCreate.process(List.of(CustomerId.random()), unknown, CustomerId.random()));

        // then
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
        expectNothingCreated();
    }

    @Test
    void rejectsACertifierWhoIsNoInstructorOfTheTool() {
        // given
        ToolView tool = givenToolWithInstructors(CustomerId.random());

        // when
        Throwable thrown = catchThrowable(() -> this.toolCertificationCreate.process(List.of(CustomerId.random()), tool.id(), CustomerId.random()));

        // then
        assertThat(thrown) //
                .isInstanceOf(CustomerNotInstructorException.class) //
                .hasFieldOrPropertyWithValue("code", CustomerNotInstructorException.CODE);
        expectNothingCreated();
    }

    @Test
    void rejectsAnUnknownCustomerAndCertifiesNobody() {
        // given
        CustomerId instructorId = CustomerId.random();
        ToolView tool = givenToolWithInstructors(instructorId);
        CustomerId customerId = givenCustomer();
        CustomerId unknown = givenUnknownCustomer();

        // when
        Throwable thrown = catchThrowable(() -> this.toolCertificationCreate.process(List.of(customerId, unknown), tool.id(), instructorId));

        // then
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
        expectNothingCreated();
    }

    @Test
    void rejectsAnInstructorOfTheToolAndCertifiesNobody() {
        // given
        CustomerId instructorId = CustomerId.random();
        CustomerId otherInstructorId = givenCustomer();
        ToolView tool = givenToolWithInstructors(instructorId, otherInstructorId);
        CustomerId customerId = givenCustomer();

        // when
        Throwable thrown = catchThrowable(() -> this.toolCertificationCreate.process(List.of(customerId, otherInstructorId), tool.id(), instructorId));

        // then
        assertThat(thrown) //
                .isInstanceOf(CustomerIsInstructorException.class) //
                .hasFieldOrPropertyWithValue("code", CustomerIsInstructorException.CODE);
        expectNothingCreated();
    }

    // ------------------------------------------------------------------------
    // given

    private ToolView givenToolWithInstructors(CustomerId... instructorIds) {
        ToolView tool = toolView().instructors(Set.of(instructorIds)).build();
        when(this.toolService.getTool(tool.id())).thenReturn(Optional.of(tool));

        return tool;
    }

    private ToolId givenUnknownTool() {
        ToolId unknown = ToolId.random();
        when(this.toolService.getTool(unknown)).thenReturn(Optional.empty());

        return unknown;
    }

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

    private void expectNothingCreated() {
        verifyNoInteractions(this.toolCertificationService);
    }

}
