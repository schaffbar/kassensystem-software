package de.schaffbar.core_pos.shared.id;

import java.util.Locale;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Value;

import de.schaffbar.core_pos.shared.id.ValueObjectAssert.ValueObjectName;

@Value(staticConstructor = "of")
public class MacAddress implements ValueObject<MacAddress> {

    private static final Pattern FORMAT = Pattern.compile("([0-9A-F]{2}:){5}[0-9A-F]{2}");

    @JsonValue
    String value;

    // the readers send upper case (34:CD:B0:D3:A9:30); lower case is accepted and stored upper case, so lookups match
    @JsonCreator
    private MacAddress(String macAddress) {
        ValueObjectAssert.notBlank(macAddress, ValueObjectName.MAC_ADDRESS);
        String upperCase = macAddress.toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(upperCase).matches()) {
            throw new IllegalArgumentException("Invalid MAC address format: " + macAddress + " (expected like 34:CD:B0:D3:A9:30)");
        }
        this.value = upperCase;
    }

}
