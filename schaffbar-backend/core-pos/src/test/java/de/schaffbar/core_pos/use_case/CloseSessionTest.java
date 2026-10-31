package de.schaffbar.core_pos.use_case;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import lombok.Builder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.schaffbar.core_pos.domain.customer.CustomerService;
import de.schaffbar.core_pos.domain.customer.CustomerViews.CustomerAddressView;
import de.schaffbar.core_pos.domain.customer.CustomerViews.CustomerView;
import de.schaffbar.core_pos.domain.tool_usage.ToolUsageService;
import de.schaffbar.core_pos.domain.tool_usage.ToolUsageViews.ToolUsageView;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionService;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionStatus;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionViews.WorkshopSessionView;
import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageService;
import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageViews.WorkshopUsageView;
import de.schaffbar.core_pos.shared.billing.BillingTotal;
import de.schaffbar.core_pos.shared.exception.ActiveToolUsageExistsForSessionException;
import de.schaffbar.core_pos.shared.exception.ActiveWorkshopUsageExistsForSessionException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;
import de.schaffbar.core_pos.shared.id.ToolUsageId;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;
import de.schaffbar.core_pos.shared.id.WorkshopUsageId;

@ExtendWith(MockitoExtension.class)
class CloseSessionTest {

    private static final Instant START = Instant.parse("2026-10-31T10:00:00Z");

    private static final Instant END = Instant.parse("2026-10-31T11:00:00Z");

    private final CustomerId customerId = CustomerId.random();

    private final WorkshopSessionId sessionId = WorkshopSessionId.random();

    @Mock
    private CustomerService customerService;

    @Mock
    private WorkshopSessionService workshopSessionService;

    @Mock
    private WorkshopUsageService workshopUsageService;

    @Mock
    private ToolUsageService toolUsageService;

    @InjectMocks
    private CloseSession closeSession;

    @BeforeEach
    void setUp() {
        when(this.customerService.getCustomer(this.customerId)).thenReturn(Optional.of(customer()));
        when(this.workshopSessionService.getOpenWorkshopSession(this.customerId)).thenReturn(Optional.of(openSession()));
    }

    @Test
    void closesTheSessionWithTheTotalsOfAllUsages() {
        // given
        BillingTotal workshopTotal = new BillingTotal(60, 10);
        BillingTotal toolTotal = new BillingTotal(31, 6);
        when(this.workshopUsageService.getWorkshopUsages(this.sessionId)).thenReturn(List.of(workshopUsage(END)));
        when(this.toolUsageService.getToolUsages(this.sessionId)).thenReturn(List.of(toolUsage(END)));
        when(this.workshopUsageService.getBillingTotal(eq(this.sessionId), any())).thenReturn(workshopTotal);
        when(this.toolUsageService.getBillingTotal(eq(this.sessionId), any())).thenReturn(toolTotal);

        // when
        this.closeSession.process(this.customerId);

        // then
        verify(this.workshopSessionService).closeSession(this.customerId, workshopTotal, toolTotal);
    }

    @Test
    void refusesToCloseWhileAWorkshopUsageIsRunning() {
        // given
        when(this.toolUsageService.getToolUsages(this.sessionId)).thenReturn(List.of());
        when(this.workshopUsageService.getWorkshopUsages(this.sessionId)).thenReturn(List.of(workshopUsage(null)));

        // when / then
        assertThatThrownBy(() -> this.closeSession.process(this.customerId)) //
                .isInstanceOf(ActiveWorkshopUsageExistsForSessionException.class);
        verify(this.workshopSessionService, never()).closeSession(any(), any(), any());
    }

    @Test
    void refusesToCloseWhileAToolUsageIsRunning() {
        // given
        when(this.toolUsageService.getToolUsages(this.sessionId)).thenReturn(List.of(toolUsage(null)));

        // when / then
        assertThatThrownBy(() -> this.closeSession.process(this.customerId)) //
                .isInstanceOf(ActiveToolUsageExistsForSessionException.class);
        verify(this.workshopSessionService, never()).closeSession(any(), any(), any());
    }

    // ------------------------------------------------------------------------
    // helper

    private CustomerView customer() {
        CustomerAddressView address = new CustomerAddressView("Hauptstraße 1", null, "14467", "Potsdam", "Deutschland");
        return new CustomerView(this.customerId, "Max", "Mustermann", LocalDate.of(1990, 1, 31), false, "max@example.com", null, address, START, START);
    }

    private WorkshopSessionView openSession() {
        return new WorkshopSessionView(this.sessionId, this.customerId, START, null, WorkshopSessionStatus.OPEN);
    }

    /**
     * {@code exitTime == null}: the usage is still running.
     */
    private WorkshopUsageView workshopUsage(Instant exitTime) {
        return new WorkshopUsageView(WorkshopUsageId.random(), this.customerId, this.sessionId, START, exitTime, null);
    }

    /**
     * {@code endTime == null}: the usage is still running.
     */
    private ToolUsageView toolUsage(Instant endTime) {
        return new ToolUsageView(ToolUsageId.random(), this.customerId, ToolId.random(), this.sessionId, START, endTime, null);
    }

    @Builder
    record Scenario( //
            CustomerId customerId, //
            WorkshopSessionId sessionId, //
            Instant start, //
            Instant end //
    ) {

        static ScenarioBuilder random() {
            return Scenario.builder() //
                    .customerId(CustomerId.random()) //
                    .sessionId(WorkshopSessionId.random()) //
                    .start(Instant.now()) //
                    .end(Instant.now());
        }

    }

}
