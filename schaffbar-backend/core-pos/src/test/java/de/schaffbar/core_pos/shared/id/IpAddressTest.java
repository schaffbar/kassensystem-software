package de.schaffbar.core_pos.shared.id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

// fixed values on purpose: the formats are what this test is about
class IpAddressTest {

    @ParameterizedTest
    @ValueSource(strings = { "192.168.0.10", "192.168.35.111", "10.0.0.1", "0.0.0.0", "255.255.255.255" })
    void of_ipv4Address_isKept(String ipv4Address) {
        // when
        IpAddress ipAddress = IpAddress.of(ipv4Address);

        // then
        assertThat(ipAddress.getValue()).isEqualTo(ipv4Address);
    }

    @ParameterizedTest
    @ValueSource(strings = { "192.168.0.256", "192.168.0.300", "192.168.00.10", "192.168.010.10", "192.168.0", "192.168.0.10.1", "192.168.0.",
            "192.168..10", "192.168.0.1a", " 192.168.0.10", "192.168.0.10 ", "::1" })
    void of_otherFormat_isRejected(String otherFormat) {
        // when
        Throwable thrown = catchThrowable(() -> IpAddress.of(otherFormat));

        // then
        assertThat(thrown) //
                .isInstanceOf(IllegalArgumentException.class) //
                .hasMessageContaining(otherFormat);
    }

    // Integer.parseInt accepts a sign and digits of other scripts
    @ParameterizedTest
    @ValueSource(strings = { "+1.+2.+3.+4", "-0.-0.-0.-0", "192.168.0.+10", "١٩٢.168.0.10" })
    void of_notOnlyAsciiDigits_isRejected(String notOnlyAsciiDigits) {
        // when
        Throwable thrown = catchThrowable(() -> IpAddress.of(notOnlyAsciiDigits));

        // then
        assertThat(thrown) //
                .isInstanceOf(IllegalArgumentException.class) //
                .hasMessageContaining(notOnlyAsciiDigits);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    void of_blank_isRejected(String blank) {
        // when
        Throwable thrown = catchThrowable(() -> IpAddress.of(blank));

        // then
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

}
