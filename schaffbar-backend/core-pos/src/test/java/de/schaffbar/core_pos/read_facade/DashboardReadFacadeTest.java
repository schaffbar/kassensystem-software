package de.schaffbar.core_pos.read_facade;

import static de.schaffbar.core_pos.builder.ViewBuilder.customerView;
import static de.schaffbar.core_pos.builder.ViewBuilder.runningToolUsageView;
import static de.schaffbar.core_pos.builder.ViewBuilder.runningWorkshopUsageView;
import static de.schaffbar.core_pos.builder.ViewBuilder.toolView;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.schaffbar.core_pos.domain.customer.CustomerService;
import de.schaffbar.core_pos.domain.customer.CustomerViews.CustomerView;
import de.schaffbar.core_pos.domain.tool.ToolService;
import de.schaffbar.core_pos.domain.tool.ToolViews.ToolView;
import de.schaffbar.core_pos.domain.tool_usage.ToolUsageService;
import de.schaffbar.core_pos.domain.tool_usage.ToolUsageViews.ToolUsageView;
import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageService;
import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageViews.WorkshopUsageView;
import de.schaffbar.core_pos.read_facade.DashboardViews.ActiveToolUsageView;
import de.schaffbar.core_pos.read_facade.DashboardViews.ActiveWorkshopUserView;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;

@ExtendWith(MockitoExtension.class)
class DashboardReadFacadeTest {

    @Mock
    private WorkshopUsageService workshopUsageService;

    @Mock
    private ToolUsageService toolUsageService;

    @Mock
    private ToolService toolService;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private DashboardReadFacade dashboardReadFacade;

    // ------------------------------------------------------------------------
    // getActiveWorkshopUsers

    @Test
    void getActiveWorkshopUsers_showsEverybodyCheckedInTheLongestThereFirst() {
        // given
        CustomerView latest = givenCustomer();
        CustomerView longest = givenCustomer();
        Instant now = Instant.now();
        WorkshopUsageView tenMinutes = runningWorkshopUsageView().customerId(latest.id()).entryTime(now.minusSeconds(600)).build();
        WorkshopUsageView anHour = runningWorkshopUsageView().customerId(longest.id()).entryTime(now.minusSeconds(3600)).build();
        givenRunningWorkshopUsages(tenMinutes, anHour);

        // when
        List<ActiveWorkshopUserView> users = this.dashboardReadFacade.getActiveWorkshopUsers();

        // then
        assertThat(users) //
                .extracting(ActiveWorkshopUserView::customerId, ActiveWorkshopUserView::firstName, ActiveWorkshopUserView::lastName,
                        ActiveWorkshopUserView::entryTime) //
                .containsExactly( //
                        tuple(longest.id(), longest.firstName(), longest.lastName(), anHour.entryTime()), //
                        tuple(latest.id(), latest.firstName(), latest.lastName(), tenMinutes.entryTime()));
    }

    @Test
    void getActiveWorkshopUsers_withoutTheCustomerOfAUsage_isABug() {
        // given
        WorkshopUsageView usage = runningWorkshopUsageView().build();
        givenRunningWorkshopUsages(usage);
        givenNoCustomer(usage.customerId());

        // when
        Throwable thrown = catchThrowable(() -> this.dashboardReadFacade.getActiveWorkshopUsers());

        // then
        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }

    // ------------------------------------------------------------------------
    // getActiveToolUsages

    @Test
    void getActiveToolUsages_showsEveryRunningToolWithThePersonUsingIt() {
        // given
        ToolView tool = givenTool();
        CustomerView customer = givenCustomer();
        ToolUsageView usage = runningToolUsageView(tool.id()).customerId(customer.id()).build();
        givenRunningToolUsages(usage);

        // when
        List<ActiveToolUsageView> toolUsages = this.dashboardReadFacade.getActiveToolUsages();

        // then
        assertThat(toolUsages) //
                .extracting(ActiveToolUsageView::toolId, ActiveToolUsageView::toolName, ActiveToolUsageView::customerId, ActiveToolUsageView::firstName,
                        ActiveToolUsageView::lastName) //
                .containsExactly(tuple(tool.id(), tool.name(), customer.id(), customer.firstName(), customer.lastName()));
    }

    @Test
    void getActiveToolUsages_withoutTheToolOfAUsage_isABug() {
        // given
        ToolUsageView usage = runningToolUsageView(ToolId.random()).build();
        givenRunningToolUsages(usage);
        givenNoTool(usage.toolId());

        // when
        Throwable thrown = catchThrowable(() -> this.dashboardReadFacade.getActiveToolUsages());

        // then
        assertThat(thrown) //
                .isInstanceOf(IllegalStateException.class) //
                .hasMessageContaining(usage.toolId().getValue().toString());
    }

    @Test
    void getActiveToolUsages_withoutTheCustomerOfAUsage_isABug() {
        // given
        ToolView tool = givenTool();
        ToolUsageView usage = runningToolUsageView(tool.id()).build();
        givenRunningToolUsages(usage);
        givenNoCustomer(usage.customerId());

        // when
        Throwable thrown = catchThrowable(() -> this.dashboardReadFacade.getActiveToolUsages());

        // then
        assertThat(thrown) //
                .isInstanceOf(IllegalStateException.class) //
                .hasMessageContaining(usage.customerId().getValue().toString());
    }

    // ------------------------------------------------------------------------
    // given

    private void givenRunningWorkshopUsages(WorkshopUsageView... usages) {
        when(this.workshopUsageService.getRunningWorkshopUsages()).thenReturn(List.of(usages));
    }

    private void givenRunningToolUsages(ToolUsageView... usages) {
        when(this.toolUsageService.getRunningToolUsages()).thenReturn(List.of(usages));
    }

    private CustomerView givenCustomer() {
        CustomerView customer = customerView().build();
        when(this.customerService.getCustomer(customer.id())).thenReturn(Optional.of(customer));

        return customer;
    }

    private ToolView givenTool() {
        ToolView tool = toolView().build();
        when(this.toolService.getTool(tool.id())).thenReturn(Optional.of(tool));

        return tool;
    }

    // broken data – the usage points to a customer that does not exist
    private void givenNoCustomer(CustomerId customerId) {
        when(this.customerService.getCustomer(customerId)).thenReturn(Optional.empty());
    }

    // broken data – the usage points to a tool that does not exist
    private void givenNoTool(ToolId toolId) {
        when(this.toolService.getTool(toolId)).thenReturn(Optional.empty());
    }

}
