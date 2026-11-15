package de.schaffbar.core_pos.integration.fixture;

import lombok.Builder;

import de.schaffbar.core_pos.builder.RandomValues;
import de.schaffbar.core_pos.shared.id.MacAddress;

// random values instead of cleaning up: the tests share one database and never see each other's data
@Builder
public record Scenario( //
        MacAddress macAddress, //
        String toolName //
) {

    public static ScenarioBuilder randomValues() {
        return Scenario.builder() //
                .macAddress(RandomValues.macAddress()) //
                .toolName(RandomValues.toolName());
    }

}
