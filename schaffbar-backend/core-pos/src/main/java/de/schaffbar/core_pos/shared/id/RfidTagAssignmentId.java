package de.schaffbar.core_pos.shared.id;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Value;

import de.schaffbar.core_pos.shared.id.ValueObjectAssert.ValueObjectName;

@Value(staticConstructor = "of")
public class RfidTagAssignmentId implements ValueObject<RfidTagAssignmentId> {

    @JsonValue
    UUID value;

    @JsonCreator
    private RfidTagAssignmentId(UUID id) {
        ValueObjectAssert.notNull(id, ValueObjectName.RFID_TAG_ASSIGNMENT_ID);
        this.value = id;
    }

    public static RfidTagAssignmentId random() {
        return RfidTagAssignmentId.of(UUID.randomUUID());
    }

}
