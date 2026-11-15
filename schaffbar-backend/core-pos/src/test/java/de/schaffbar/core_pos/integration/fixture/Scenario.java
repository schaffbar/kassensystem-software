package de.schaffbar.core_pos.integration.fixture;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.Builder;

// random values instead of cleaning up: the tests share one database and never see each other's data
@Builder
public record Scenario( //
        String macAddress, //
        String toolName //
) {

    public static ScenarioBuilder randomValues() {
        return Scenario.builder() //
                .macAddress(randomMacAddress()) //
                .toolName("Werkzeug " + UUID.randomUUID());
    }

    // ------------------------------------------------------------------------
    // helper

    private static String randomMacAddress() {
        return IntStream.range(0, 6) //
                .mapToObj(_ -> "%02X".formatted(ThreadLocalRandom.current().nextInt(256))) //
                .collect(Collectors.joining(":"));
    }

}
