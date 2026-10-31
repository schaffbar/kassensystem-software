package de.schaffbar.core_pos.domain.workshop_session.web;

import static java.util.Objects.isNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import de.schaffbar.core_pos.domain.tool.ToolViews.ToolView;
import de.schaffbar.core_pos.domain.tool_usage.ToolUsageViews.ToolUsageView;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionStatus;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionViews.WorkshopSessionView;
import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageViews.WorkshopUsageView;
import de.schaffbar.core_pos.shared.billing.BillingTotal;
import de.schaffbar.core_pos.shared.id.ToolId;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface WorkshopSessionApiModel {

    WorkshopSessionApiModel MAPPER = Mappers.getMapper(WorkshopSessionApiModel.class);

    // ------------------------------------------------------------------------
    // mapping view to response

    @Mapping(target = "id", source = "session.id.value")
    @Mapping(target = "customerId", source = "session.customerId.value")
    @Mapping(target = "workshopUsages", source = "workshopUsages")
    @Mapping(target = "toolUsageSummaries", source = "toolUsageSummaries")
    @Mapping(target = "totalWorkshopDurationInMinutes", source = "workshopTotal.minutes")
    @Mapping(target = "totalWorkshopUnits", source = "workshopTotal.units")
    WorkshopSessionApiDto toWorkshopSessionApiDto(WorkshopSessionView session, List<WorkshopUsageView> workshopUsages, BillingTotal workshopTotal,
            List<ToolUsageSummaryApiDto> toolUsageSummaries);

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "durationInMinutes", source = "billableDuration.minutes")
    @Mapping(target = "unitsUsed", source = "billableDuration.units")
    WorkshopUsageApiDto toWorkshopUsageApiDto(WorkshopUsageView workshopUsage);

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "durationInMinutes", source = "billableDuration.minutes")
    @Mapping(target = "unitsUsed", source = "billableDuration.units")
    ToolUsageDetailApiDto toToolUsageDetailApiDto(ToolUsageView usage);

    // ------------------------------------------------------------------------
    // tool usage grouping – the totals per tool come from the ToolUsageService

    default List<ToolUsageSummaryApiDto> toToolUsageSummaries(List<ToolUsageView> toolUsages, Map<ToolId, ToolView> toolMap,
            Map<ToolId, BillingTotal> totalsByTool) {
        return toolUsages.stream() //
                .collect(Collectors.groupingBy(ToolUsageView::toolId)) //
                .entrySet().stream() //
                .map(entry -> {
                    ToolId toolId = entry.getKey();
                    List<ToolUsageView> usagesForTool = entry.getValue();
                    ToolView tool = toolMap.get(toolId);

                    String toolName = (tool != null) ? tool.name() : null;

                    List<ToolUsageDetailApiDto> details = usagesForTool.stream() //
                            .map(this::toToolUsageDetailApiDto) //
                            .toList();

                    BillingTotal total = totalsByTool.getOrDefault(toolId, BillingTotal.ZERO);

                    boolean active = usagesForTool.stream().anyMatch(u -> isNull(u.endTime()));

                    return new ToolUsageSummaryApiDto(toolId.getValue().toString(), toolName, active, total.minutes(), total.units(), details);
                }) //
                .toList();
    }

    // ------------------------------------------------------------------------
    // mapping request body to command

    // ------------------------------------------------------------------------
    // response

    record WorkshopSessionApiDto( //
            @NotBlank String id, //
            @NotBlank String customerId, //
            @NotNull Instant startTime, //
            Instant closeTime, //
            @NotNull WorkshopSessionStatus status, //
            @PositiveOrZero long totalWorkshopDurationInMinutes, //
            @PositiveOrZero long totalWorkshopUnits, //
            List<WorkshopUsageApiDto> workshopUsages, //
            List<ToolUsageSummaryApiDto> toolUsageSummaries //
    ) {}

    record WorkshopUsageApiDto( //
            @NotNull String id, //
            @NotNull @PastOrPresent Instant entryTime, //
            @PastOrPresent Instant exitTime, //
            @Positive Long durationInMinutes, //
            @Positive Long unitsUsed //
    ) {}

    record ToolUsageSummaryApiDto( //
            @NotBlank String toolId, //
            String toolName, //
            boolean active, //
            @Positive Long totalDurationInMinutes, //
            @Positive Long totalUnits, //
            List<ToolUsageDetailApiDto> usages //
    ) {}

    record ToolUsageDetailApiDto( //
            @NotBlank String id, //
            @NotNull @PastOrPresent Instant startTime, //
            @PastOrPresent Instant endTime, //
            @Positive Long durationInMinutes, //
            @Positive Long unitsUsed //
    ) {}

    // ------------------------------------------------------------------------
    // request body

}
