package de.schaffbar.core_pos.domain.tool;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class WlanRelaisTypeTest {

    // T-6 – the switch box calls <protocol>://<ipAddress><path><onCommand or offCommand>: the HTTP API of the relay
    @ParameterizedTest
    @CsvSource({ "SHELLY_1, http://192.168.1.10/relay/0?turn=on, http://192.168.1.10/relay/0?turn=off",
            "SHELLY_2, http://192.168.1.10/relay/0?turn=on, http://192.168.1.10/relay/0?turn=off",
            "SHELLY_PRO, http://192.168.1.10/rpc/Switch.Set?id=0&on=true, http://192.168.1.10/rpc/Switch.Set?id=0&on=false" })
    void values_areTheHttpApiOfTheRelay(WlanRelaisType type, String expectedOnUrl, String expectedOffUrl) {
        // when – put together like the switch box does it
        String onUrl = type.getProtocol() + "://192.168.1.10" + type.getPath() + type.getOnCommand();
        String offUrl = type.getProtocol() + "://192.168.1.10" + type.getPath() + type.getOffCommand();

        // then
        assertThat(onUrl).isEqualTo(expectedOnUrl);
        assertThat(offUrl).isEqualTo(expectedOffUrl);
    }

}
