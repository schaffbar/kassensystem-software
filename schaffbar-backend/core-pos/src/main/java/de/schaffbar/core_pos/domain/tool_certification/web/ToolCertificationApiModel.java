package de.schaffbar.core_pos.domain.tool_certification.web;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationStatus;
import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationViews.ToolCertificationView;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolCertificationId;
import de.schaffbar.core_pos.shared.id.ToolId;
import de.schaffbar.core_pos.shared.id.ValueObjectMapper;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ToolCertificationApiModel extends ValueObjectMapper {

    ToolCertificationApiModel MAPPER = Mappers.getMapper(ToolCertificationApiModel.class);

    // ------------------------------------------------------------------------
    // mapping view to response

    ToolCertificationApiDto toToolCertificationApiDto(ToolCertificationView view);

    // ------------------------------------------------------------------------
    // response

    record ToolCertificationApiDto( //
            @NotNull ToolCertificationId id, //
            @NotNull CustomerId customerId, //
            @NotNull ToolId toolId, //
            @NotNull ToolCertificationStatus status, //
            @NotNull Instant certifiedAt, //
            CustomerId certifiedBy //
    ) {}

    // ------------------------------------------------------------------------
    // request

    record BatchCertifyRequestBody( //
            @NotEmpty List<@NotNull @Valid CustomerId> customerIds, //
            @NotNull @Valid ToolId toolId, //
            @NotNull @Valid CustomerId certifiedBy //
    ) {}

}
