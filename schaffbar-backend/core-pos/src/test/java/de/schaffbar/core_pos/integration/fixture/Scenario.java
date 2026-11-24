package de.schaffbar.core_pos.integration.fixture;

import lombok.Builder;

import de.schaffbar.core_pos.builder.RandomValues;
import de.schaffbar.core_pos.shared.id.IpAddress;
import de.schaffbar.core_pos.shared.id.MacAddress;
import de.schaffbar.core_pos.shared.id.RfidTagId;

// random values instead of cleaning up: the tests share one database and never see each other's data
@Builder
public record Scenario( //
        MacAddress macAddress, //
        String rfidReaderName, //
        RfidTagId rfidTagId, //
        String toolName, //
        IpAddress ipAddress //
) {

    public static ScenarioBuilder randomValues() {
        return Scenario.builder() //
                .macAddress(RandomValues.macAddress()) //
                .rfidReaderName(RandomValues.rfidReaderName()) //
                .rfidTagId(RandomValues.rfidTagId()) //
                .toolName(RandomValues.toolName()) //
                .ipAddress(RandomValues.ipAddress());
    }

}
