package de.schaffbar.core_pos.web.dashboard;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import de.schaffbar.core_pos.read_facade.DashboardViews.ActiveToolUsageView;
import de.schaffbar.core_pos.read_facade.DashboardViews.ActiveWorkshopUserView;
import de.schaffbar.core_pos.shared.id.ValueObjectMapper;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DashboardApiModel extends ValueObjectMapper {

    DashboardApiModel MAPPER = Mappers.getMapper(DashboardApiModel.class);

    // ------------------------------------------------------------------------
    // mapping view to response

    @Mapping(target = "customerId", source = "customerId.value")
    ActiveWorkshopUserApiDto toActiveWorkshopUserApiDto(ActiveWorkshopUserView view);

    @Mapping(target = "toolId", source = "toolId.value")
    @Mapping(target = "customerId", source = "customerId.value")
    ActiveToolUsageApiDto toActiveToolUsageApiDto(ActiveToolUsageView view);

    // ------------------------------------------------------------------------
    // response

    record ActiveWorkshopUserApiDto( //
            @NotBlank String customerId, //
            @NotBlank String firstName, //
            @NotBlank String lastName, //
            @NotNull @PastOrPresent Instant entryTime //
    ) {}

    record ActiveToolUsageApiDto( //
            @NotBlank String toolId, //
            @NotBlank String toolName, //
            @NotBlank String customerId, //
            @NotBlank String firstName, //
            @NotBlank String lastName //
    ) {}

}
