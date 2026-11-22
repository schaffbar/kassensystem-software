package de.schaffbar.core_pos.domain.tool_certification.web;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationStatus;
import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationViews.ToolCertificationView;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;
import de.schaffbar.core_pos.shared.id.ValueObjectMapper;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ToolCertificationApiModel extends ValueObjectMapper {

    ToolCertificationApiModel MAPPER = Mappers.getMapper(ToolCertificationApiModel.class);

    // ------------------------------------------------------------------------
    // mapping view to response

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "customerId", source = "customerId.value")
    @Mapping(target = "toolId", source = "toolId.value")
    @Mapping(target = "certifiedBy", source = "certifiedBy.value")
    ToolCertificationApiDto toToolCertificationApiDto(ToolCertificationView view);

    // ------------------------------------------------------------------------
    // response

    record ToolCertificationApiDto( //
            @NotBlank String id, //
            @NotBlank String customerId, //
            @NotBlank String toolId, //
            @NotNull ToolCertificationStatus status, //
            @NotNull Instant certifiedAt, //
            String certifiedBy //
    ) {}

    // ------------------------------------------------------------------------
    // request

    record BatchCertifyRequestBody( //
            @NotEmpty List<@NotNull @Valid CustomerId> customerIds, //
            @NotNull @Valid ToolId toolId, //
            @NotNull @Valid CustomerId certifiedBy //
    ) {}

}
