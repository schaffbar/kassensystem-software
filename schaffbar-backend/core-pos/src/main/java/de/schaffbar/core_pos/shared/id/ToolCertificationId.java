package de.schaffbar.core_pos.shared.id;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Value;

import de.schaffbar.core_pos.shared.id.ValueObjectAssert.ValueObjectName;

@Value(staticConstructor = "of")
public class ToolCertificationId implements ValueObject<ToolCertificationId> {

    @JsonValue
    UUID value;

    @JsonCreator
    private ToolCertificationId(UUID id) {
        ValueObjectAssert.notNull(id, ValueObjectName.TOOL_CERTIFICATION_ID);
        this.value = id;
    }

    public static ToolCertificationId random() {
        return ToolCertificationId.of(UUID.randomUUID());
    }

}
