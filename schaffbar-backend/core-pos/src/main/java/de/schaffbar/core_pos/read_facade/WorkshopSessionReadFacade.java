package de.schaffbar.core_pos.read_facade;

import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.groupingBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.customer.CustomerService;
import de.schaffbar.core_pos.domain.tool.ToolService;
import de.schaffbar.core_pos.domain.tool.ToolViews.ToolView;
import de.schaffbar.core_pos.domain.tool_usage.ToolUsageService;
import de.schaffbar.core_pos.domain.tool_usage.ToolUsageViews.ToolUsageView;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionService;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionViews.WorkshopSessionView;
import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageService;
import de.schaffbar.core_pos.read_facade.OpenWorkshopSessionViews.OpenWorkshopSessionView;
import de.schaffbar.core_pos.read_facade.OpenWorkshopSessionViews.ToolUsageSummaryView;
import de.schaffbar.core_pos.shared.billing.BillingTotal;
import de.schaffbar.core_pos.shared.cqrs.Query;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;

@Service
@Validated
@RequiredArgsConstructor
public class WorkshopSessionReadFacade {

    private final @NonNull WorkshopSessionService workshopSessionService;

    private final @NonNull WorkshopUsageService workshopUsageService;

    private final @NonNull ToolUsageService toolUsageService;

    private final @NonNull ToolService toolService;

    private final @NonNull CustomerService customerService;

    @Query
    public Optional<OpenWorkshopSessionView> getOpenWorkshopSession(@NotNull @Valid CustomerId customerId) {
        this.customerService.getCustomer(customerId) //
                .orElseThrow(() -> ResourceNotFoundException.customer(customerId));

        return this.workshopSessionService.getOpenWorkshopSession(customerId) //
                .map(this::toOpenWorkshopSessionView);
    }

    // ------------------------------------------------------------------------
    // helper

    private OpenWorkshopSessionView toOpenWorkshopSessionView(WorkshopSessionView session) {
        // one point in time for all running usages, so the totals fit together
        Instant now = Instant.now();

        return OpenWorkshopSessionView.builder() //
                .session(session) //
                .workshopUsages(this.workshopUsageService.getWorkshopUsages(session.id())) //
                .workshopTotal(this.workshopUsageService.getBillingTotalUntil(session.id(), now)) //
                .toolUsageSummaries(toToolUsageSummaryViews(session, now)) //
                .build();
    }

    // one summary per tool, in the order the tools were first switched on
    private List<ToolUsageSummaryView> toToolUsageSummaryViews(WorkshopSessionView session, Instant now) {
        Map<ToolId, BillingTotal> totals = this.toolUsageService.getBillingTotalsByToolUntil(session.id(), now);

        return this.toolUsageService.getToolUsages(session.id()).stream() //
                .collect(groupingBy(ToolUsageView::toolId)) //
                .entrySet().stream() //
                .map(entry -> toToolUsageSummaryView(entry.getKey(), entry.getValue(), totals.get(entry.getKey()))) //
                .sorted(comparing(summary -> summary.usages().getFirst().startTime())) //
                .toList();
    }

    private ToolUsageSummaryView toToolUsageSummaryView(ToolId toolId, List<ToolUsageView> usages, BillingTotal total) {
        return ToolUsageSummaryView.builder() //
                .toolId(toolId) //
                .toolName(this.toolService.getTool(toolId).map(ToolView::name).orElse(null)) //
                .active(usages.stream().anyMatch(usage -> isNull(usage.endTime()))) //
                .total(total) //
                .usages(usages) //
                .build();
    }

}
