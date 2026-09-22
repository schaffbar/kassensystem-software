package de.schaffbar.core_pos.shared.id;

import static java.util.Objects.isNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import de.schaffbar.core_pos.shared.id.ValueObjectAssert.ValueObject;
import lombok.Value;

@Value(staticConstructor = "of")
public class MacAddress {

    @JsonValue
    String value;

    @JsonCreator
    private MacAddress(String macAddress) {
        ValueObjectAssert.notBlank(macAddress, ValueObject.MAC_ADDRESS);
        // TODO: validate MAC address format
        this.value = macAddress;
    }

    public boolean sameValueAs(MacAddress other) {
        if (isNull(other)) {
            return false;
        }

        return this.value.equals(other.value);
    }

}
