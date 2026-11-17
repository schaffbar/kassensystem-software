package de.schaffbar.core_pos.domain.tool;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.shared.id.IpAddress;
import de.schaffbar.core_pos.shared.id.ToolId;

public interface ToolCommands {

    @Builder
    @Validated
    record CreateToolCommand( //
            @NotBlank String name, //
            String description, //
            @NotNull ToolArea area, //
            @NotNull CertificationRequirement certificationRequirement //
    ) {}

    @Builder
    @Validated
    record UpdateToolCommand( //
            @NotNull @Valid ToolId id, //
            @NotBlank String name, //
            String description, //
            @NotNull ToolArea area, //
            @NotNull CertificationRequirement certificationRequirement //
    ) {}

    @Builder
    @Validated
    record SetWlanRelaisCommand( //
            @NotNull @Valid ToolId id, //
            @NotNull WlanRelaisType wlanRelaisType, //
            @NotNull @Valid IpAddress ipAddress //
    ) {}

}
