package de.schaffbar.core_pos.shared.id;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Value;

import de.schaffbar.core_pos.shared.id.ValueObjectAssert.ValueObjectName;

@Value(staticConstructor = "of")
public class CustomerId implements ValueObject<CustomerId> {

    @JsonValue
    UUID value;

    @JsonCreator
    private CustomerId(UUID id) {
        ValueObjectAssert.notNull(id, ValueObjectName.CUSTOMER_ID);
        this.value = id;
    }

    public static CustomerId random() {
        return CustomerId.of(UUID.randomUUID());
    }

}
