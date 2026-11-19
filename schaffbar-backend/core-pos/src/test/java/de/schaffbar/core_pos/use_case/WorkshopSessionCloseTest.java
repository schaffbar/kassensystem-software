package de.schaffbar.core_pos.use_case;

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
import de.schaffbar.core_pos.shared.exception.ActiveToolUsageExistsForSessionException;
import de.schaffbar.core_pos.shared.exception.ActiveWorkshopUsageExistsForSessionException;
import de.schaffbar.core_pos.shared.exception.NoOpenWorkshopSessionException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
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
        when(this.workshopUsageService.getBillingTotal(open.id())).thenReturn(workshopTotal);
        when(this.toolUsageService.getBillingTotal(open.id())).thenReturn(toolTotal);

        // when
        this.workshopSessionClose.process(open.customerId());

        // then
        verify(this.workshopSessionService).closeWorkshopSession(open.customerId(), workshopTotal, toolTotal);
    }

    @Test
    void rejectsASessionWithARunningWorkshopUsage_beforeLookingAtTheTools() {
        // given – checking out stops the tools too, so it is the answer
        WorkshopSessionView open = givenOpenSession();
        when(this.workshopUsageService.hasActiveWorkshopUsage(open.id())).thenReturn(true);

        // when
        Throwable thrown = catchThrowable(() -> this.workshopSessionClose.process(open.customerId()));

        // then
        assertThat(thrown) //
                .isInstanceOf(ActiveWorkshopUsageExistsForSessionException.class) //
                .hasFieldOrPropertyWithValue("code", ActiveWorkshopUsageExistsForSessionException.CODE);
        verifyNoInteractions(this.toolUsageService);
        expectNotClosed();
    }

    @Test
    void rejectsASessionWithARunningToolUsage() {
        // given
        WorkshopSessionView open = givenOpenSession();
        when(this.workshopUsageService.hasActiveWorkshopUsage(open.id())).thenReturn(false);
        when(this.toolUsageService.hasActiveToolUsage(open.id())).thenReturn(true);

        // when
        Throwable thrown = catchThrowable(() -> this.workshopSessionClose.process(open.customerId()));

        // then
        assertThat(thrown) //
                .isInstanceOf(ActiveToolUsageExistsForSessionException.class) //
                .hasFieldOrPropertyWithValue("code", ActiveToolUsageExistsForSessionException.CODE);
        expectNotClosed();
    }

    @Test
    void rejectsACustomerWithoutOpenSession() {
        // given
        CustomerId customerId = givenCustomer();
        when(this.workshopSessionService.getOpenWorkshopSession(customerId)).thenReturn(Optional.empty());

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
        CustomerId unknown = CustomerId.random();
        when(this.customerService.getCustomer(unknown)).thenReturn(Optional.empty());

        // when
        Throwable thrown = catchThrowable(() -> this.workshopSessionClose.process(unknown));

        // then
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(this.workshopSessionService);
    }

    // ------------------------------------------------------------------------
    // given

    private CustomerId givenCustomer() {
        CustomerId customerId = CustomerId.random();
        when(this.customerService.getCustomer(customerId)).thenReturn(Optional.of(customerView(customerId).build()));

        return customerId;
    }

    private WorkshopSessionView givenOpenSession() {
        WorkshopSessionView open = openWorkshopSessionView().customerId(givenCustomer()).build();
        when(this.workshopSessionService.getOpenWorkshopSession(open.customerId())).thenReturn(Optional.of(open));

        return open;
    }

    private WorkshopSessionView givenOpenSessionWithoutRunningUsages() {
        WorkshopSessionView open = givenOpenSession();
        when(this.workshopUsageService.hasActiveWorkshopUsage(open.id())).thenReturn(false);
        when(this.toolUsageService.hasActiveToolUsage(open.id())).thenReturn(false);

        return open;
    }

    // ------------------------------------------------------------------------
    // then

    private void expectNotClosed() {
        verify(this.workshopSessionService, never()).closeWorkshopSession(any(), any(), any());
    }

}
