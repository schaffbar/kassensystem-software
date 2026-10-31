package de.schaffbar.core_pos.domain.tool;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.IpAddress;
import de.schaffbar.core_pos.shared.id.RfidReaderId;
import de.schaffbar.core_pos.shared.id.ToolId;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ToolViews {

    ToolViews MAPPER = Mappers.getMapper(ToolViews.class);

    // ------------------------------------------------------------------------
    // mapper

    ToolView toToolView(Tool tool);

    // ------------------------------------------------------------------------
    // views

    @Builder
    @Validated
    record ToolView( //
            @NotNull ToolId id, //
            @NotBlank String name, //
            String description, //
            @NotNull ToolArea area, //
            @NotNull CertificationRequirement certificationRequirement, //
            RfidReaderId rfidReaderId, //
            WlanRelaisType wlanRelaisType, //
            IpAddress ipAddress, //
            String httpStartCommand, //
            String onCommand, //
            String offCommand, //
            Set<CustomerId> instructors //
    ) {}

}
