package de.schaffbar.core_pos.shared.id;

import static java.util.Objects.isNull;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Value;

import de.schaffbar.core_pos.shared.id.ValueObjectAssert.ValueObject;

@Value(staticConstructor = "of")
public class ToolId {

    @JsonValue
    UUID value;

    @JsonCreator
    private ToolId(UUID id) {
        ValueObjectAssert.notNull(id, ValueObject.TOOL_ID);
        this.value = id;
    }

    public static ToolId random() {
        return ToolId.of(UUID.randomUUID());
    }

    public boolean sameValueAs(ToolId other) {
        if (isNull(other)) {
            return false;
        }

        return this.value.equals(other.value);
    }

}
