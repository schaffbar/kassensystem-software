package de.schaffbar.core_pos.shared.id;

import static java.util.Objects.isNull;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Value;

import de.schaffbar.core_pos.shared.id.ValueObjectAssert.ValueObject;

@Value(staticConstructor = "of")
public class WorkshopSessionId {

    @JsonValue
    UUID value;

    @JsonCreator
    private WorkshopSessionId(UUID id) {
        ValueObjectAssert.notNull(id, ValueObject.CUSTOMER_ID);
        this.value = id;
    }

    public static WorkshopSessionId random() {
        return WorkshopSessionId.of(UUID.randomUUID());
    }

    public boolean sameValueAs(WorkshopSessionId other) {
        if (isNull(other)) {
            return false;
        }

        return this.value.equals(other.value);
    }

}
