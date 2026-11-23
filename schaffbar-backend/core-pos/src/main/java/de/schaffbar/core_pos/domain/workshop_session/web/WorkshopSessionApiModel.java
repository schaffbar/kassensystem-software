package de.schaffbar.core_pos.domain.workshop_session.web;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import de.schaffbar.core_pos.domain.tool_usage.ToolUsageViews.ToolUsageView;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionStatus;
import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageViews.WorkshopUsageView;
import de.schaffbar.core_pos.read_facade.OpenWorkshopSessionViews.OpenWorkshopSessionView;
import de.schaffbar.core_pos.read_facade.OpenWorkshopSessionViews.ToolUsageSummaryView;
import de.schaffbar.core_pos.shared.id.ValueObjectMapper;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface WorkshopSessionApiModel extends ValueObjectMapper {

    WorkshopSessionApiModel MAPPER = Mappers.getMapper(WorkshopSessionApiModel.class);

    // ------------------------------------------------------------------------
    // mapping view to response

    @Mapping(target = "id", source = "session.id.value")
    @Mapping(target = "customerId", source = "session.customerId.value")
    @Mapping(target = "startTime", source = "session.startTime")
    @Mapping(target = "closeTime", source = "session.closeTime")
    @Mapping(target = "status", source = "session.status")
    @Mapping(target = "totalWorkshopDurationInMinutes", source = "workshopTotal.minutes")
    @Mapping(target = "totalWorkshopUnits", source = "workshopTotal.units")
    WorkshopSessionApiDto toWorkshopSessionApiDto(OpenWorkshopSessionView openWorkshopSession);

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "durationInMinutes", source = "billableDuration.minutes")
    @Mapping(target = "unitsUsed", source = "billableDuration.units")
    WorkshopUsageApiDto toWorkshopUsageApiDto(WorkshopUsageView workshopUsage);

    @Mapping(target = "toolId", source = "toolId.value")
    @Mapping(target = "totalDurationInMinutes", source = "total.minutes")
    @Mapping(target = "totalUnits", source = "total.units")
    ToolUsageSummaryApiDto toToolUsageSummaryApiDto(ToolUsageSummaryView toolUsageSummary);

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "durationInMinutes", source = "billableDuration.minutes")
    @Mapping(target = "unitsUsed", source = "billableDuration.units")
    ToolUsageDetailApiDto toToolUsageDetailApiDto(ToolUsageView toolUsage);

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
            @NotNull List<WorkshopUsageApiDto> workshopUsages, //
            @NotNull List<ToolUsageSummaryApiDto> toolUsageSummaries //
    ) {}

    record WorkshopUsageApiDto( //
            @NotBlank String id, //
            @NotNull @PastOrPresent Instant entryTime, //
            @PastOrPresent Instant exitTime, //
            @Positive Long durationInMinutes, //
            @Positive Long unitsUsed //
    ) {}

    record ToolUsageSummaryApiDto( //
            @NotBlank String toolId, //
            String toolName, //
            boolean active, //
            @PositiveOrZero long totalDurationInMinutes, //
            @PositiveOrZero long totalUnits, //
            @NotNull List<ToolUsageDetailApiDto> usages //
    ) {}

    record ToolUsageDetailApiDto( //
            @NotBlank String id, //
            @NotNull @PastOrPresent Instant startTime, //
            @PastOrPresent Instant endTime, //
            @Positive Long durationInMinutes, //
            @Positive Long unitsUsed //
    ) {}

}
