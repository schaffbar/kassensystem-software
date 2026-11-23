package de.schaffbar.core_pos.read_facade;

import static de.schaffbar.core_pos.builder.ViewBuilder.customerView;
import static de.schaffbar.core_pos.builder.ViewBuilder.finishedToolUsageView;
import static de.schaffbar.core_pos.builder.ViewBuilder.openWorkshopSessionView;
import static de.schaffbar.core_pos.builder.ViewBuilder.runningToolUsageView;
import static de.schaffbar.core_pos.builder.ViewBuilder.runningWorkshopUsageView;
import static de.schaffbar.core_pos.builder.ViewBuilder.toolView;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.schaffbar.core_pos.domain.customer.CustomerService;
import de.schaffbar.core_pos.domain.tool.ToolService;
import de.schaffbar.core_pos.domain.tool.ToolViews.ToolView;
import de.schaffbar.core_pos.domain.tool_usage.ToolUsageService;
import de.schaffbar.core_pos.domain.tool_usage.ToolUsageViews.ToolUsageView;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionService;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionViews.WorkshopSessionView;
import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageService;
import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageViews.WorkshopUsageView;
import de.schaffbar.core_pos.read_facade.OpenWorkshopSessionViews.OpenWorkshopSessionView;
import de.schaffbar.core_pos.read_facade.OpenWorkshopSessionViews.ToolUsageSummaryView;
import de.schaffbar.core_pos.shared.billing.BillingTotal;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;

@ExtendWith(MockitoExtension.class)
class WorkshopSessionReadFacadeTest {

    @Mock
    private WorkshopSessionService workshopSessionService;

    @Mock
    private WorkshopUsageService workshopUsageService;

    @Mock
    private ToolUsageService toolUsageService;

    @Mock
    private ToolService toolService;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private WorkshopSessionReadFacade workshopSessionReadFacade;

    // ------------------------------------------------------------------------
    // getOpenWorkshopSession

    @Test
    void getOpenWorkshopSession_combinesTheUsagesAndTheirTotals() {
        // given
        WorkshopUsageView workshopUsage = runningWorkshopUsageView().build();
        BillingTotal workshopTotal = new BillingTotal(61, 11);
        WorkshopSessionView session = givenOpenSession(List.of(workshopUsage), workshopTotal, List.of(), Map.of());

        // when
        Optional<OpenWorkshopSessionView> result = this.workshopSessionReadFacade.getOpenWorkshopSession(session.customerId());

        // then
        assertThat(result).hasValueSatisfying(open -> {
            assertThat(open.session()).isEqualTo(session);
            assertThat(open.workshopUsages()).containsExactly(workshopUsage);
            assertThat(open.workshopTotal()).isEqualTo(workshopTotal);
            assertThat(open.toolUsageSummaries()).isEmpty();
        });
    }

    @Test
    void getOpenWorkshopSession_summarizesTheToolUsagesPerToolInTheOrderTheToolsWereFirstSwitchedOn() {
        // given – the drill was used once, the saw was used and runs again
        ToolView drill = givenTool();
        ToolView saw = givenTool();
        Instant start = Instant.now().minusSeconds(3600);
        ToolUsageView drillUsage = finishedToolUsageView(drill.id()).startTime(start).build();
        ToolUsageView firstSawUsage = finishedToolUsageView(saw.id()).startTime(start.plusSeconds(60)).build();
        ToolUsageView runningSawUsage = runningToolUsageView(saw.id()).build();
        BillingTotal drillTotal = new BillingTotal(7, 2);
        BillingTotal sawTotal = new BillingTotal(19, 4);
        WorkshopSessionView session = givenOpenSession(List.of(), BillingTotal.ZERO, List.of(drillUsage, firstSawUsage, runningSawUsage),
                Map.of(drill.id(), drillTotal, saw.id(), sawTotal));

        // when
        Optional<OpenWorkshopSessionView> result = this.workshopSessionReadFacade.getOpenWorkshopSession(session.customerId());

        // then
        assertThat(result.orElseThrow().toolUsageSummaries()) //
                .extracting(ToolUsageSummaryView::toolId, ToolUsageSummaryView::toolName, ToolUsageSummaryView::active, ToolUsageSummaryView::total,
                        ToolUsageSummaryView::usages) //
                .containsExactly( //
                        tuple(drill.id(), drill.name(), false, drillTotal, List.of(drillUsage)), //
                        tuple(saw.id(), saw.name(), true, sawTotal, List.of(firstSawUsage, runningSawUsage)));
    }

    @Test
    void getOpenWorkshopSession_showsADeletedToolWithoutName() {
        // given
        ToolId deleted = givenDeletedTool();
        WorkshopSessionView session = givenOpenSession(List.of(), BillingTotal.ZERO, List.of(finishedToolUsageView(deleted).build()),
                Map.of(deleted, new BillingTotal(7, 2)));

        // when
        Optional<OpenWorkshopSessionView> result = this.workshopSessionReadFacade.getOpenWorkshopSession(session.customerId());

        // then
        assertThat(result.orElseThrow().toolUsageSummaries()) //
                .singleElement() //
                .satisfies(summary -> {
                    assertThat(summary.toolId()).isEqualTo(deleted);
                    assertThat(summary.toolName()).isNull();
                });
    }

    @Test
    void getOpenWorkshopSession_withoutOpenSession_isEmpty() {
        // given
        CustomerId customerId = givenCustomerWithoutOpenSession();

        // when
        Optional<OpenWorkshopSessionView> result = this.workshopSessionReadFacade.getOpenWorkshopSession(customerId);

        // then
        assertThat(result).isEmpty();
        verifyNoInteractions(this.workshopUsageService, this.toolUsageService);
    }

    @Test
    void getOpenWorkshopSession_ofAnUnknownCustomer_isRejected() {
        // given
        CustomerId unknown = givenUnknownCustomer();

        // when
        Throwable thrown = catchThrowable(() -> this.workshopSessionReadFacade.getOpenWorkshopSession(unknown));

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

    private WorkshopSessionView givenOpenSession(List<WorkshopUsageView> workshopUsages, BillingTotal workshopTotal, List<ToolUsageView> toolUsages,
            Map<ToolId, BillingTotal> toolTotals) {
        WorkshopSessionView session = openWorkshopSessionView().customerId(givenCustomer()).build();
        when(this.workshopSessionService.getOpenWorkshopSession(session.customerId())).thenReturn(Optional.of(session));
        when(this.workshopUsageService.getWorkshopUsages(session.id())).thenReturn(workshopUsages);
        when(this.workshopUsageService.getBillingTotalUntil(eq(session.id()), any())).thenReturn(workshopTotal);
        when(this.toolUsageService.getToolUsages(session.id())).thenReturn(toolUsages);
        when(this.toolUsageService.getBillingTotalsByToolUntil(eq(session.id()), any())).thenReturn(toolTotals);

        return session;
    }

    private ToolView givenTool() {
        ToolView tool = toolView().build();
        when(this.toolService.getTool(tool.id())).thenReturn(Optional.of(tool));

        return tool;
    }

    private ToolId givenDeletedTool() {
        ToolId deleted = ToolId.random();
        when(this.toolService.getTool(deleted)).thenReturn(Optional.empty());

        return deleted;
    }

}
