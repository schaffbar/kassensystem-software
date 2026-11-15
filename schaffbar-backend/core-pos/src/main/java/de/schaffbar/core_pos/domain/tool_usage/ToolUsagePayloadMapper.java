package de.schaffbar.core_pos.domain.tool_usage;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import de.schaffbar.core_pos.shared.event.payload.ToolUsageEventPayload.ToolUsageStartedPayload;
import de.schaffbar.core_pos.shared.event.payload.ToolUsageEventPayload.ToolUsageStoppedPayload;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
interface ToolUsagePayloadMapper {

    ToolUsagePayloadMapper MAPPER = Mappers.getMapper(ToolUsagePayloadMapper.class);

    // ------------------------------------------------------------------------
    // mapper

    ToolUsageStartedPayload toToolUsageStartedPayload(ToolUsage toolUsage);

    ToolUsageStoppedPayload toToolUsageStoppedPayload(ToolUsage toolUsage);

}
