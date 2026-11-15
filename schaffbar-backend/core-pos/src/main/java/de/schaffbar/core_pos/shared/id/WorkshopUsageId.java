package de.schaffbar.core_pos.shared.id;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Value;

import de.schaffbar.core_pos.shared.id.ValueObjectAssert.ValueObjectName;

@Value(staticConstructor = "of")
public class WorkshopUsageId implements ValueObject<WorkshopUsageId> {

    @JsonValue
    UUID value;

    @JsonCreator
    private WorkshopUsageId(UUID id) {
        ValueObjectAssert.notNull(id, ValueObjectName.WORKSHOP_USAGE_ID);
        this.value = id;
    }

    public static WorkshopUsageId random() {
        return WorkshopUsageId.of(UUID.randomUUID());
    }

}
