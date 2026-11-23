package de.schaffbar.core_pos.use_case;

import static de.schaffbar.core_pos.builder.CommandBuilder.closeWorkshopSessionCommand;
import static de.schaffbar.core_pos.builder.ViewBuilder.customerView;
import static de.schaffbar.core_pos.builder.ViewBuilder.openWorkshopSessionView;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import de.schaffbar.core_pos.domain.tool_usage.ToolUsageService;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionService;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionViews.WorkshopSessionView;
import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageService;
import de.schaffbar.core_pos.shared.billing.BillingTotal;
import de.schaffbar.core_pos.shared.exception.NoOpenWorkshopSessionException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException.Resource;
import de.schaffbar.core_pos.shared.exception.RunningToolUsageExistsForSessionException;
import de.schaffbar.core_pos.shared.exception.RunningWorkshopUsageExistsForSessionException;
import de.schaffbar.core_pos.shared.id.CustomerId;

@ExtendWith(MockitoExtension.class)
class WorkshopSessionCloseTest {

    @Mock
    private CustomerService customerService;

    @Mock
    private WorkshopSessionService workshopSessionService;

    @Mock
    private WorkshopUsageService workshopUsageService;

    @Mock
    private ToolUsageService toolUsageService;

    @InjectMocks
    private WorkshopSessionClose workshopSessionClose;

    @Test
    void closesTheSessionWithTheFinalTotals() {
        // given
        WorkshopSessionView open = givenOpenSessionWithoutRunningUsages();
        BillingTotal workshopTotal = new BillingTotal(60, 10);
        BillingTotal toolTotal = new BillingTotal(31, 6);
        givenBillingTotals(open, workshopTotal, toolTotal);

        // when
        this.workshopSessionClose.process(open.customerId());

        // then
        expectClosed(open, workshopTotal, toolTotal);
    }

    @Test
    void rejectsASessionWithARunningWorkshopUsage_beforeLookingAtTheTools() {
        // given – checking out stops the tools too, so it is the answer
        WorkshopSessionView open = givenOpenSessionWithRunningWorkshopUsage();

        // when
        Throwable thrown = catchThrowable(() -> this.workshopSessionClose.process(open.customerId()));

        // then
        assertThat(thrown) //
                .isInstanceOf(RunningWorkshopUsageExistsForSessionException.class) //
                .hasFieldOrPropertyWithValue("code", RunningWorkshopUsageExistsForSessionException.CODE);
        verifyNoInteractions(this.toolUsageService);
        expectNotClosed();
    }

    @Test
    void rejectsASessionWithARunningToolUsage() {
        // given
        WorkshopSessionView open = givenOpenSessionWithRunningToolUsage();

        // when
        Throwable thrown = catchThrowable(() -> this.workshopSessionClose.process(open.customerId()));

        // then
        assertThat(thrown) //
                .isInstanceOf(RunningToolUsageExistsForSessionException.class) //
                .hasFieldOrPropertyWithValue("code", RunningToolUsageExistsForSessionException.CODE);
        expectNotClosed();
    }

    @Test
    void rejectsACustomerWithoutOpenSession() {
        // given
        CustomerId customerId = givenCustomerWithoutOpenSession();

        // when
        Throwable thrown = catchThrowable(() -> this.workshopSessionClose.process(customerId));

        // then
        assertThat(thrown) //
                .isInstanceOf(NoOpenWorkshopSessionException.class) //
                .hasFieldOrPropertyWithValue("code", NoOpenWorkshopSessionException.CODE);
        expectNotClosed();
    }

    @Test
    void rejectsAnUnknownCustomer() {
        // given
        CustomerId unknown = givenUnknownCustomer();

        // when
        Throwable thrown = catchThrowable(() -> this.workshopSessionClose.process(unknown));

        // then
        assertThat(thrown) //
                .isInstanceOf(ResourceNotFoundException.class) //
                .hasFieldOrPropertyWithValue("resource", Resource.CUSTOMER);
        expectNoSessionLookedUp();
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

    private WorkshopSessionView givenOpenSession() {
        WorkshopSessionView open = openWorkshopSessionView().customerId(givenCustomer()).build();
        when(this.workshopSessionService.getOpenWorkshopSession(open.customerId())).thenReturn(Optional.of(open));

        return open;
    }

    private WorkshopSessionView givenOpenSessionWithoutRunningUsages() {
        WorkshopSessionView open = givenOpenSession();
        when(this.workshopUsageService.hasRunningWorkshopUsage(open.id())).thenReturn(false);
        when(this.toolUsageService.hasRunningToolUsage(open.id())).thenReturn(false);

        return open;
    }

    private WorkshopSessionView givenOpenSessionWithRunningWorkshopUsage() {
        WorkshopSessionView open = givenOpenSession();
        when(this.workshopUsageService.hasRunningWorkshopUsage(open.id())).thenReturn(true);

        return open;
    }

    private WorkshopSessionView givenOpenSessionWithRunningToolUsage() {
        WorkshopSessionView open = givenOpenSession();
        when(this.workshopUsageService.hasRunningWorkshopUsage(open.id())).thenReturn(false);
        when(this.toolUsageService.hasRunningToolUsage(open.id())).thenReturn(true);

        return open;
    }

    private void givenBillingTotals(WorkshopSessionView session, BillingTotal workshopTotal, BillingTotal toolTotal) {
        when(this.workshopUsageService.getBillingTotal(session.id())).thenReturn(workshopTotal);
        when(this.toolUsageService.getBillingTotal(session.id())).thenReturn(toolTotal);
    }

    // ------------------------------------------------------------------------
    // then

    private void expectClosed(WorkshopSessionView open, BillingTotal workshopTotal, BillingTotal toolTotal) {
        verify(this.workshopSessionService).closeWorkshopSession(closeWorkshopSessionCommand(open.id()) //
                .workshopTotal(workshopTotal) //
                .toolTotal(toolTotal) //
                .build());
    }

    private void expectNotClosed() {
        verify(this.workshopSessionService, never()).closeWorkshopSession(any());
    }

    private void expectNoSessionLookedUp() {
        verifyNoInteractions(this.workshopSessionService);
    }

}
