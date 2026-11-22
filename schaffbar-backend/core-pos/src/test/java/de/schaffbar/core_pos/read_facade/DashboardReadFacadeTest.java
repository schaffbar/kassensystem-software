package de.schaffbar.core_pos.read_facade;

import static de.schaffbar.core_pos.builder.ViewBuilder.activeToolUsageView;
import static de.schaffbar.core_pos.builder.ViewBuilder.activeWorkshopUsageView;
import static de.schaffbar.core_pos.builder.ViewBuilder.customerView;
import static de.schaffbar.core_pos.builder.ViewBuilder.toolView;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
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
        CustomerView anna = givenCustomer("Anna");
        CustomerView ben = givenCustomer("Ben");
        Instant now = Instant.now();
        WorkshopUsageView annaSinceTen = activeWorkshopUsageView().customerId(anna.id()).entryTime(now.minusSeconds(600)).build();
        WorkshopUsageView benSinceAnHour = activeWorkshopUsageView().customerId(ben.id()).entryTime(now.minusSeconds(3600)).build();
        givenActiveWorkshopUsages(annaSinceTen, benSinceAnHour);

        // when
        List<ActiveWorkshopUserView> users = this.dashboardReadFacade.getActiveWorkshopUsers();

        // then
        assertThat(users).containsExactly( //
                new ActiveWorkshopUserView(ben.id(), "Ben", ben.lastName(), benSinceAnHour.entryTime()), //
                new ActiveWorkshopUserView(anna.id(), "Anna", anna.lastName(), annaSinceTen.entryTime()));
    }

    @Test
    void getActiveWorkshopUsers_withoutTheCustomerOfAUsage_isABug() {
        // given
        WorkshopUsageView usage = activeWorkshopUsageView().build();
        givenActiveWorkshopUsages(usage);
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
        ToolView saw = givenTool("Kreissäge");
        CustomerView anna = givenCustomer("Anna");
        ToolUsageView usage = activeToolUsageView(saw.id()).customerId(anna.id()).build();
        givenActiveToolUsages(usage);

        // when
        List<ActiveToolUsageView> toolUsages = this.dashboardReadFacade.getActiveToolUsages();

        // then
        assertThat(toolUsages).containsExactly(new ActiveToolUsageView(saw.id(), "Kreissäge", anna.id(), "Anna", anna.lastName()));
    }

    @Test
    void getActiveToolUsages_withoutTheToolOfAUsage_isABug() {
        // given
        ToolUsageView usage = activeToolUsageView(ToolId.random()).build();
        givenActiveToolUsages(usage);
        givenNoTool(usage.toolId());

        // when
        Throwable thrown = catchThrowable(() -> this.dashboardReadFacade.getActiveToolUsages());

        // then
        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getActiveToolUsages_withoutTheCustomerOfAUsage_isABug() {
        // given
        ToolView saw = givenTool("Kreissäge");
        ToolUsageView usage = activeToolUsageView(saw.id()).build();
        givenActiveToolUsages(usage);
        givenNoCustomer(usage.customerId());

        // when
        Throwable thrown = catchThrowable(() -> this.dashboardReadFacade.getActiveToolUsages());

        // then
        assertThat(thrown).isInstanceOf(IllegalStateException.class);
    }

    // ------------------------------------------------------------------------
    // given

    private void givenActiveWorkshopUsages(WorkshopUsageView... usages) {
        when(this.workshopUsageService.getActiveWorkshopUsages()).thenReturn(List.of(usages));
    }

    private void givenActiveToolUsages(ToolUsageView... usages) {
        when(this.toolUsageService.getActiveToolUsages()).thenReturn(List.of(usages));
    }

    private CustomerView givenCustomer(String firstName) {
        CustomerView customer = customerView().firstName(firstName).build();
        when(this.customerService.getCustomer(customer.id())).thenReturn(Optional.of(customer));

        return customer;
    }

    private ToolView givenTool(String name) {
        ToolView tool = toolView().name(name).build();
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
