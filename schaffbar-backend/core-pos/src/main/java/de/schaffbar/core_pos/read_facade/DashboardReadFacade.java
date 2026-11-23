package de.schaffbar.core_pos.read_facade;

import static java.util.Comparator.comparing;

import java.util.List;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

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
import de.schaffbar.core_pos.shared.cqrs.Query;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;

@Service
@Validated
@RequiredArgsConstructor
public class DashboardReadFacade {

    private final @NonNull WorkshopUsageService workshopUsageService;

    private final @NonNull ToolUsageService toolUsageService;

    private final @NonNull ToolService toolService;

    private final @NonNull CustomerService customerService;

    // everybody checked in right now, the longest there first
    @Query
    public List<ActiveWorkshopUserView> getActiveWorkshopUsers() {
        return this.workshopUsageService.getActiveWorkshopUsages().stream() //
                .sorted(comparing(WorkshopUsageView::entryTime)) //
                .map(this::toActiveWorkshopUserView) //
                .toList();
    }

    // every tool switched on right now, with the person using it
    @Query
    public List<ActiveToolUsageView> getActiveToolUsages() {
        return this.toolUsageService.getRunningToolUsages().stream() //
                .map(this::toActiveToolUsageView) //
                .toList();
    }

    // ------------------------------------------------------------------------
    // helper

    private ActiveWorkshopUserView toActiveWorkshopUserView(WorkshopUsageView usage) {
        CustomerView customer = getCustomer(usage.customerId());

        return ActiveWorkshopUserView.builder() //
                .customerId(customer.id()) //
                .firstName(customer.firstName()) //
                .lastName(customer.lastName()) //
                .entryTime(usage.entryTime()) //
                .build();
    }

    private ActiveToolUsageView toActiveToolUsageView(ToolUsageView usage) {
        ToolView tool = getTool(usage.toolId());
        CustomerView customer = getCustomer(usage.customerId());

        return ActiveToolUsageView.builder() //
                .toolId(tool.id()) //
                .toolName(tool.name()) //
                .customerId(customer.id()) //
                .firstName(customer.firstName()) //
                .lastName(customer.lastName()) //
                .build();
    }

    // a tool in use cannot be deleted (ToolDelete)
    private ToolView getTool(ToolId toolId) {
        return this.toolService.getTool(toolId) //
                .orElseThrow(() -> new IllegalStateException("Tool [id: " + toolId.getValue() + "] of a running tool usage does not exist"));
    }

    // a customer with an open session cannot be deleted (CustomerDelete)
    private CustomerView getCustomer(CustomerId customerId) {
        return this.customerService.getCustomer(customerId) //
                .orElseThrow(() -> new IllegalStateException("Customer [id: " + customerId.getValue() + "] of a running usage does not exist"));
    }

}
