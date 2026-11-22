package de.schaffbar.core_pos.shared.id;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Value;

import de.schaffbar.core_pos.shared.id.ValueObjectAssert.ValueObjectName;

@Value(staticConstructor = "of")
public class RfidReaderId implements ValueObject<RfidReaderId> {

    @JsonValue
    UUID value;

    @JsonCreator
    private RfidReaderId(UUID id) {
        ValueObjectAssert.notNull(id, ValueObjectName.RFID_READER_ID);
        this.value = id;
    }

    public static RfidReaderId random() {
        return RfidReaderId.of(UUID.randomUUID());
    }

}
