package de.schaffbar.core_pos.read_facade;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.tool_usage.ToolUsageViews.ToolUsageView;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionViews.WorkshopSessionView;
import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageViews.WorkshopUsageView;
import de.schaffbar.core_pos.shared.billing.BillingTotal;
import de.schaffbar.core_pos.shared.id.ToolId;

public interface OpenWorkshopSessionViews {

    // the totals count the running usages until the moment the view was read
    @Builder
    @Validated
    record OpenWorkshopSessionView( //
            @Valid @NotNull WorkshopSessionView session, //
            @NotNull List<@Valid @NotNull WorkshopUsageView> workshopUsages, //
            @Valid @NotNull BillingTotal workshopTotal, //
            @NotNull List<@Valid @NotNull ToolUsageSummaryView> toolUsageSummaries //
    ) {}

    @Builder
    @Validated
    record ToolUsageSummaryView( //
            @Valid @NotNull ToolId toolId, //
            String toolName, // null when the tool was deleted
            boolean active, //
            @Valid @NotNull BillingTotal total, //
            @NotNull List<@Valid @NotNull ToolUsageView> usages //
    ) {}

}
