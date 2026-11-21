package de.schaffbar.core_pos.domain.tool;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class WlanRelaisTypeTest {

    @ParameterizedTest
    @EnumSource(WlanRelaisType.class)
    void url_isTheProtocolTheIpAddressThePathAndTheQuery(WlanRelaisType type) {
        // when
        String onUrl = type.getProtocol() + "://192.168.1.10" + type.getPath() + type.getOnCommand();
        String offUrl = type.getProtocol() + "://192.168.1.10" + type.getPath() + type.getOffCommand();

        // then
        assertThat(onUrl).matches("http://192\\.168\\.1\\.10/[A-Za-z0-9/.]+\\?[a-z0-9=&]+");
        assertThat(offUrl).matches("http://192\\.168\\.1\\.10/[A-Za-z0-9/.]+\\?[a-z0-9=&]+");
    }

}
