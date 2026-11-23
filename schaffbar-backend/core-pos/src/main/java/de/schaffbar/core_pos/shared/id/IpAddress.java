package de.schaffbar.core_pos.shared.id;

import java.util.Arrays;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Value;

import de.schaffbar.core_pos.shared.id.ValueObjectAssert.ValueObjectName;

@Value(staticConstructor = "of")
public class IpAddress implements ValueObject<IpAddress> {

    // ASCII digits only, no sign and no leading zero – Integer.parseInt alone accepts "+1" and "-0"
    private static final Pattern OCTET = Pattern.compile("0|[1-9][0-9]{0,2}");

    @JsonValue
    String value;

    @JsonCreator
    private IpAddress(String ipAddress) {
        ValueObjectAssert.notBlank(ipAddress, ValueObjectName.IP_ADDRESS);
        if (!isValidIpv4(ipAddress)) {
            throw new IllegalArgumentException("Invalid IP address format: " + ipAddress);
        }
        this.value = ipAddress;
    }

    // ------------------------------------------------------------------------
    // helper

    private static boolean isValidIpv4(String ipAddress) {
        String[] octets = ipAddress.split("\\.", -1);
        return octets.length == 4 && Arrays.stream(octets).allMatch(octet -> OCTET.matcher(octet).matches() && Integer.parseInt(octet) <= 255);
    }

}
