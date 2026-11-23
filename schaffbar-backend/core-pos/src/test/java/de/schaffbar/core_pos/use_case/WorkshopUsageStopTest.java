package de.schaffbar.core_pos.use_case;

import static de.schaffbar.core_pos.builder.ViewBuilder.customerView;
import static de.schaffbar.core_pos.builder.ViewBuilder.openWorkshopSessionView;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.schaffbar.core_pos.domain.customer.CustomerService;
import de.schaffbar.core_pos.domain.tool_usage.ToolUsageService;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionService;
import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageService;
import de.schaffbar.core_pos.shared.exception.NoOpenWorkshopSessionException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException.Resource;
import de.schaffbar.core_pos.shared.id.CustomerId;

@ExtendWith(MockitoExtension.class)
class WorkshopUsageStopTest {

    @Mock
    private CustomerService customerService;

    @Mock
    private WorkshopSessionService workshopSessionService;

    @Mock
    private WorkshopUsageService workshopUsageService;

    @Mock
    private ToolUsageService toolUsageService;

    @InjectMocks
    private WorkshopUsageStop workshopUsageStop;

    @Test
    void stopsTheToolUsagesAndThenTheWorkshopUsage() {
        // given
        CustomerId customerId = givenCustomerWithOpenSession();

        // when
        this.workshopUsageStop.process(customerId);

        // then
        expectToolsStoppedBeforeTheUsage(customerId);
    }

    @Test
    void rejectsACustomerWithoutOpenSession() {
        // given
        CustomerId customerId = givenCustomerWithoutOpenSession();

        // when
        Throwable thrown = catchThrowable(() -> this.workshopUsageStop.process(customerId));

        // then
        assertThat(thrown) //
                .isInstanceOf(NoOpenWorkshopSessionException.class) //
                .hasFieldOrPropertyWithValue("code", NoOpenWorkshopSessionException.CODE);
        expectNothingStopped();
    }

    @Test
    void rejectsAnUnknownCustomer() {
        // given
        CustomerId unknown = givenUnknownCustomer();

        // when
        Throwable thrown = catchThrowable(() -> this.workshopUsageStop.process(unknown));

        // then
        assertThat(thrown) //
                .isInstanceOf(ResourceNotFoundException.class) //
                .hasFieldOrPropertyWithValue("resource", Resource.CUSTOMER);
        expectNothingStopped();
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

    // ------------------------------------------------------------------------
    // then

    private void expectToolsStoppedBeforeTheUsage(CustomerId customerId) {
        InOrder order = inOrder(this.toolUsageService, this.workshopUsageService);
        order.verify(this.toolUsageService).stopToolUsages(customerId);
        order.verify(this.workshopUsageService).stopWorkshopUsage(customerId);
    }

    private void expectNothingStopped() {
        verifyNoInteractions(this.toolUsageService, this.workshopUsageService);
    }

}
