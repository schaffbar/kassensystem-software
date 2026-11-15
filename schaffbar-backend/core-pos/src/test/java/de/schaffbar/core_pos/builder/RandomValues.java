package de.schaffbar.core_pos.builder;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import de.schaffbar.core_pos.shared.id.MacAddress;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RandomValues {

    public static MacAddress macAddress() {
        return MacAddress.of(IntStream.range(0, 6) //
                .mapToObj(_ -> "%02X".formatted(ThreadLocalRandom.current().nextInt(256))) //
                .collect(Collectors.joining(":")));
    }

    public static String toolName() {
        return "Werkzeug " + UUID.randomUUID();
    }

}
