package de.schaffbar.core_pos.domain.tool_usage;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import de.schaffbar.core_pos.shared.billing.BillableDuration;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;
import de.schaffbar.core_pos.shared.id.ToolUsageId;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ToolUsageViews {

    ToolUsageViews MAPPER = Mappers.getMapper(ToolUsageViews.class);

    // ------------------------------------------------------------------------
    // mapper

    ToolUsageView toToolUsageView(ToolUsage toolUsage);

    // ------------------------------------------------------------------------
    // views

    record ToolUsageView( //
            @NotNull ToolUsageId id, //
            @NotNull CustomerId customerId, //
            @NotNull ToolId toolId, //
            @NotNull WorkshopSessionId workshopSessionId, //
            @NotNull @PastOrPresent Instant startTime, //
            @PastOrPresent Instant endTime, //
            BillableDuration billableDuration // null while the usage is running
    ) {}

}
