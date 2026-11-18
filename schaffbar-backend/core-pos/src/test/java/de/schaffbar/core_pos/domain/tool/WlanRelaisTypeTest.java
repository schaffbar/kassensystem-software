package de.schaffbar.core_pos.domain.tool;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class WlanRelaisTypeTest {

    @ParameterizedTest
    @EnumSource(WlanRelaisType.class)
    void httpStartCommand_isAnUrlOfTheIpAddressEndingWithTheQuery(WlanRelaisType type) {
        // when
        String httpStartCommand = type.getHttpStartCommand();

        // then
        assertThat(httpStartCommand).matches("http://\\{\\{ipAddress}}/[A-Za-z0-9/.]+\\?");
    }

}
