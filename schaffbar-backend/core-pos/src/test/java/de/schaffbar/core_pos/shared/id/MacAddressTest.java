package de.schaffbar.core_pos.shared.id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class MacAddressTest {

    @ParameterizedTest
    @ValueSource(strings = { "34:CD:B0:D3:8F:0C", "34:CD:B0:D3:A9:30", "34:CD:B0:D3:95:C0", "34:CD:B0:D3:7F:74" })
    void of_readerAddress_isKept(String macAddress) {
        assertThat(MacAddress.of(macAddress).getValue()).isEqualTo(macAddress);
    }

    @Test
    void of_lowerCase_isStoredUpperCase() {
        assertThat(MacAddress.of("34:cd:b0:d3:a9:30")).isEqualTo(MacAddress.of("34:CD:B0:D3:A9:30"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "34-CD-B0-D3-A9-30", "34CDB0D3A930", "34:CD:B0:D3:A9", "34:CD:B0:D3:A9:30:11", "34:CD:B0:D3:A9:3G", " 34:CD:B0:D3:A9:30" })
    void of_otherFormat_isRejected(String macAddress) {
        assertThatThrownBy(() -> MacAddress.of(macAddress)) //
                .isInstanceOf(IllegalArgumentException.class) //
                .hasMessageContaining(macAddress);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    void of_blank_isRejected(String macAddress) {
        assertThatThrownBy(() -> MacAddress.of(macAddress)).isInstanceOf(IllegalArgumentException.class);
    }

}
