package de.schaffbar.core_pos.integration.device;

import static de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType.GATE_KEEPER_IN;
import static de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType.GATE_KEEPER_OUT;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import de.schaffbar.core_pos.builder.RandomValues;
import de.schaffbar.core_pos.integration.IntegrationTest;
import de.schaffbar.core_pos.integration.fixture.Scenario;
import de.schaffbar.core_pos.integration.fixture.SchaffbarFixture;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.MacAddress;
import de.schaffbar.core_pos.support.Json;

@IntegrationTest
class GateKeeperInAcceptanceTest {

    @Autowired
    private SchaffbarFixture fixture;

    // ------------------------------------------------------------------------
    // init

    @Test
    void init_startsWithoutRelay() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().rfidReader(scenario.macAddress(), GATE_KEEPER_IN);

        // when
        MvcTestResult result = this.fixture.api().initDevice(scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "STATE": "START",
                    "DEVNAME": "Gate Keeper In",
                    "DEVUSECASE": "GI",
                    "ERROR": "",
                    "STARTHTTP": "",
                    "DEVIP": "",
                    "SWITCHON": "",
                    "SWITCHOFF": "",
                    "TERMINAL": ""
                }
                """);
    }

    // ------------------------------------------------------------------------
    // card

    @Test
    void card_checksInAndGreetsWithTheEntryTime() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().rfidReader(scenario.macAddress(), GATE_KEEPER_IN);
        this.fixture.given().rfidTag(scenario.rfidTagId());
        CustomerId customerId = this.fixture.given().customerWithRfidTag(scenario.rfidTagId());

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), scenario.macAddress());

        // then – the first check-in opens the session
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "GI",
                    "STATE": "END",
                    "ERROR": "",
                    "ICON": "HI",
                    "CUSTOMERNAME": "%s",
                    "UNITS": "0"
                }
                """.formatted(this.fixture.then().fullNameOf(customerId)));

        assertThat(Json.read(result, "$.CUSTOMERSTARTSTOP")).matches("\\d{2}:\\d{2}");
        assertThat(this.fixture.api().getOpenWorkshopSession(customerId)).bodyJson().extractingPath("$.status").isEqualTo("OPEN");
    }

    @Test
    void card_againAfterCheckingOut_joinsTheOpenSession() {
        Scenario scenario = Scenario.randomValues().build();
        MacAddress gateKeeperIn = scenario.macAddress();
        MacAddress gateKeeperOut = RandomValues.macAddress();

        // given
        this.fixture.given().rfidReader(gateKeeperIn, GATE_KEEPER_IN);
        this.fixture.given().rfidReader(gateKeeperOut, GATE_KEEPER_OUT);
        this.fixture.given().rfidTag(scenario.rfidTagId());

        CustomerId customerId = this.fixture.given().customerInTheWorkshop(scenario.rfidTagId(), gateKeeperIn);
        this.fixture.given().customerLeftTheWorkshop(scenario.rfidTagId(), gateKeeperOut);

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), gateKeeperIn);

        // then – UNITS counts the finished stays
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "GI",
                    "STATE": "END",
                    "ERROR": "",
                    "ICON": "HI",
                    "CUSTOMERNAME": "%s"
                }
                """.formatted(this.fixture.then().fullNameOf(customerId)));

        assertThat(Json.read(result, "$.CUSTOMERSTARTSTOP")).matches("\\d{2}:\\d{2}");
        assertThat(Json.read(result, "$.UNITS")).matches("\\d+");
        assertThat(this.fixture.api().getOpenWorkshopSession(customerId)).bodyJson().extractingPath("$.workshopUsages.length()").isEqualTo(2);
    }

    @Test
    void card_alreadyInTheWorkshop_isRefused() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().rfidReader(scenario.macAddress(), GATE_KEEPER_IN);
        this.fixture.given().rfidTag(scenario.rfidTagId());
        CustomerId customerId = this.fixture.given().customerInTheWorkshop(scenario.rfidTagId(), scenario.macAddress());

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "test",
                    "STATE": "END",
                    "ERROR": "Du schaffst schon",
                    "ICON": "STOP",
                    "CUSTOMERNAME": "%s",
                    "UNITS": "0"
                }
                """.formatted(this.fixture.then().fullNameOf(customerId)));
    }

    @Test
    void card_tagWithoutCustomer_isNotRecognized() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().rfidReader(scenario.macAddress(), GATE_KEEPER_IN);
        this.fixture.given().rfidTag(scenario.rfidTagId());

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "test",
                    "STATE": "END",
                    "ERROR": "Kein Kunde für RFID Tag",
                    "ICON": "NOREG",
                    "CUSTOMERNAME": null,
                    "UNITS": "0"
                }
                """);
    }

    @Test
    void card_unregisteredTag_isNotRecognized() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().rfidReader(scenario.macAddress(), GATE_KEEPER_IN);

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "test",
                    "STATE": "END",
                    "ERROR": "RFID Tag nicht erkannt",
                    "ICON": "NOREG",
                    "CUSTOMERNAME": null,
                    "UNITS": "0"
                }
                """);
    }

    // ------------------------------------------------------------------------
    // counter

    @Test
    void counter_isTheWrongReader() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().rfidReader(scenario.macAddress(), GATE_KEEPER_IN);

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToCounterReader(scenario.rfidTagId(), scenario.macAddress());

        // then – this reader does not belong at /counter
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "",
                    "STATE": "END",
                    "ERROR": "Falsches Lesegerät",
                    "CUSTOMERNAME": "Error"
                }
                """);
    }

}
