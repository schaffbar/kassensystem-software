package de.schaffbar.core_pos.shared.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BillableDurationTest {

    @ParameterizedTest(name = "{0} s -> {1} min")
    @CsvSource({ //
            "0,    0", //
            "1,    1", // each started minute counts
            "59,   1", //
            "60,   1", //
            "61,   2", //
            "359,  6", //
            "360,  6", //
            "361,  7", //
            "3600, 60", //
    })
    void roundsUpToFullMinutes(long seconds, long expectedMinutes) {
        // given
        BillableDuration billableDuration = BillableDuration.of(Duration.ofSeconds(seconds));

        // when / then
        assertThat(billableDuration.getMinutes()).isEqualTo(expectedMinutes);
    }

    @ParameterizedTest(name = "{0} s -> {1} units")
    @CsvSource({ //
            "0,    0", //
            "1,    1", // 1 started minute -> 1 started unit
            "300,  1", // 5 min
            "360,  1", // exactly 6 min
            "361,  2", // 6 min 1 s -> 7 min -> 2 units
            "720,  2", // exactly 12 min
            "721,  3", //
            "3600, 10", // 1 h
    })
    void countsOneUnitPerStartedSixMinutes(long seconds, long expectedUnits) {
        // given
        BillableDuration billableDuration = BillableDuration.of(Duration.ofSeconds(seconds));

        // when / then
        assertThat(billableDuration.getUnits()).isEqualTo(expectedUnits);
    }

    @Test
    void ignoresFractionsOfASecond() {
        // given
        BillableDuration billableDuration = BillableDuration.of(Duration.ofSeconds(60, 999_999_999));

        // when / then
        assertThat(billableDuration.getMinutes()).isEqualTo(1);
    }

    @Test
    void truncatesStartAndEndToFullSecondsBetweenTwoInstants() {
        // given
        Instant start = Instant.parse("2026-10-30T10:00:00.900Z");
        Instant end = Instant.parse("2026-10-30T10:01:00.100Z");

        // when
        BillableDuration duration = BillableDuration.between(start, end);

        // then
        assertThat(duration.getDuration()).isEqualTo(Duration.ofSeconds(60));
        assertThat(duration.getMinutes()).isEqualTo(1);
    }

    @Test
    void rejectsNegativeDuration() {
        assertThatThrownBy(() -> BillableDuration.of(Duration.ofSeconds(-1))) //
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> BillableDuration.of(null)) //
                .isInstanceOf(IllegalArgumentException.class);
    }

}
