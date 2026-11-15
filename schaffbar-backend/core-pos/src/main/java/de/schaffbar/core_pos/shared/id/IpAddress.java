package de.schaffbar.core_pos.shared.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Value;

import de.schaffbar.core_pos.shared.id.ValueObjectAssert.ValueObjectName;

@Value(staticConstructor = "of")
public class IpAddress implements ValueObject<IpAddress> {

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

    private static boolean isValidIpv4(String ip) {
        String[] parts = ip.split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }

        for (String part : parts) {
            try {
                int value = Integer.parseInt(part);
                if (value < 0 || value > 255) {
                    return false;
                }
                if (part.length() > 1 && part.startsWith("0")) {
                    return false;
                }
            }
            catch (NumberFormatException e) {
                return false;
            }
        }

        return true;
    }

}
