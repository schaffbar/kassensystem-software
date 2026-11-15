package de.schaffbar.core_pos.shared.id;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Value;

import de.schaffbar.core_pos.shared.id.ValueObjectAssert.ValueObjectName;

@Value(staticConstructor = "of")
public class ToolUsageId implements ValueObject<ToolUsageId> {

    @JsonValue
    UUID value;

    @JsonCreator
    private ToolUsageId(UUID id) {
        ValueObjectAssert.notNull(id, ValueObjectName.TOOL_USAGE_ID);
        this.value = id;
    }

    public static ToolUsageId random() {
        return ToolUsageId.of(UUID.randomUUID());
    }

}
