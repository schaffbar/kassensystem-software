package de.schaffbar.core_pos.shared.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Value;

import de.schaffbar.core_pos.shared.id.ValueObjectAssert.ValueObjectName;

@Value(staticConstructor = "of")
public class RfidTagId implements ValueObject<RfidTagId> {

    @JsonValue
    String value;

    @JsonCreator
    private RfidTagId(String id) {
        ValueObjectAssert.notBlank(id, ValueObjectName.RFID_TAG_ID);
        this.value = id;
    }

}
