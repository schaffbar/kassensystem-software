package de.schaffbar.core_pos.web.device;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import de.schaffbar.core_pos.web.device.DeviceApiModel.DeviceCardResponse;

// fixed times on purpose: summer and winter time are what this test is about
class DeviceApiModelTest {

    private TimeZone zoneOfTheJvm;

    // a JVM in another zone than the workshop – the device must still show the time of Berlin
    @BeforeEach
    void setUp() {
        this.zoneOfTheJvm = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
    }

    @AfterEach
    void tearDown() {
        TimeZone.setDefault(this.zoneOfTheJvm);
    }

    // ------------------------------------------------------------------------
    // enterOk

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({ //
            "2026-01-15T12:05:00Z, 13:05", // winter time, UTC+1
            "2026-07-01T12:05:00Z, 14:05", // summer time, UTC+2
            "2026-03-29T00:30:00Z, 01:30", // the night of the switch, before it
            "2026-03-29T01:30:00Z, 03:30", // and after it
    })
    void enterOk_entryTime_isShownInTheTimeOfTheWorkshop(Instant entryTime, String shown) {
        // when
        DeviceCardResponse response = DeviceCardResponse.enterOk("Erika Mustermann", 0, entryTime);

        // then
        assertThat(response.CUSTOMERSTARTSTOP()).isEqualTo(shown);
    }

    // ------------------------------------------------------------------------
    // leaveOk

    @ParameterizedTest(name = "{0} – {1} -> {2}")
    @CsvSource({ //
            "2026-01-15T12:05:00Z, 2026-01-15T14:30:00Z, 13:05 - 15:30", //
            "2026-07-01T12:05:00Z, 2026-07-01T14:30:00Z, 14:05 - 16:30", //
    })
    void leaveOk_entryAndExitTime_areShownInTheTimeOfTheWorkshop(Instant entryTime, Instant exitTime, String shown) {
        // when
        DeviceCardResponse response = DeviceCardResponse.leaveOk("Erika Mustermann", 0, entryTime, exitTime);

        // then
        assertThat(response.CUSTOMERSTARTSTOP()).isEqualTo(shown);
    }

}
