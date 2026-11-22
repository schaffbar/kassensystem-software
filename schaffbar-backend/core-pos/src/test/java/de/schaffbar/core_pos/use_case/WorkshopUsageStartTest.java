package de.schaffbar.core_pos.use_case;

import static de.schaffbar.core_pos.builder.ViewBuilder.customerView;
import static de.schaffbar.core_pos.builder.ViewBuilder.openWorkshopSessionView;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionService;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionViews.WorkshopSessionView;
import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageService;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;

@ExtendWith(MockitoExtension.class)
class WorkshopUsageStartTest {

    @Mock
    private CustomerService customerService;

    @Mock
    private WorkshopSessionService workshopSessionService;

    @Mock
    private WorkshopUsageService workshopUsageService;

    @InjectMocks
    private WorkshopUsageStart workshopUsageStart;

    @Test
    void withoutOpenSession_startsASessionAndTheUsageInIt() {
        // given
        CustomerId customerId = givenCustomerWithoutOpenSession();
        WorkshopSessionId started = givenSessionStartsFor(customerId);

        // when
        this.workshopUsageStart.process(customerId);

        // then
        InOrder order = inOrder(this.workshopSessionService, this.workshopUsageService);
        order.verify(this.workshopSessionService).startWorkshopSession(customerId);
        order.verify(this.workshopUsageService).startWorkshopUsage(customerId, started);
    }

    @Test
    void withOpenSession_startsTheUsageInIt() {
        // given
        WorkshopSessionView open = givenCustomerWithOpenSession();

        // when
        this.workshopUsageStart.process(open.customerId());

        // then
        verify(this.workshopUsageService).startWorkshopUsage(open.customerId(), open.id());
        verify(this.workshopSessionService, never()).startWorkshopSession(any());
    }

    @Test
    void rejectsAnUnknownCustomer() {
        // given
        CustomerId unknown = givenUnknownCustomer();

        // when
        Throwable thrown = catchThrowable(() -> this.workshopUsageStart.process(unknown));

        // then
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(this.workshopSessionService, this.workshopUsageService);
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

    private WorkshopSessionView givenCustomerWithOpenSession() {
        WorkshopSessionView open = openWorkshopSessionView().customerId(givenCustomer()).build();
        when(this.workshopSessionService.getOpenWorkshopSession(open.customerId())).thenReturn(Optional.of(open));

        return open;
    }

    private WorkshopSessionId givenSessionStartsFor(CustomerId customerId) {
        WorkshopSessionId started = WorkshopSessionId.random();
        when(this.workshopSessionService.startWorkshopSession(customerId)).thenReturn(started);

        return started;
    }

}
