package de.schaffbar.core_pos.shared.id;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Value;

import de.schaffbar.core_pos.shared.id.ValueObjectAssert.ValueObjectName;

@Value(staticConstructor = "of")
public class WorkshopSessionId implements ValueObject<WorkshopSessionId> {

    @JsonValue
    UUID value;

    @JsonCreator
    private WorkshopSessionId(UUID id) {
        ValueObjectAssert.notNull(id, ValueObjectName.WORKSHOP_SESSION_ID);
        this.value = id;
    }

    public static WorkshopSessionId random() {
        return WorkshopSessionId.of(UUID.randomUUID());
    }

}
