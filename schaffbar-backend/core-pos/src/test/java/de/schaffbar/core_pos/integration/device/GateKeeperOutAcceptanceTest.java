package de.schaffbar.core_pos.integration.device;

import static de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType.GATE_KEEPER_OUT;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import de.schaffbar.core_pos.builder.RandomValues;
import de.schaffbar.core_pos.domain.tool.CertificationRequirement;
import de.schaffbar.core_pos.integration.IntegrationTest;
import de.schaffbar.core_pos.integration.fixture.Scenario;
import de.schaffbar.core_pos.integration.fixture.SchaffbarFixture;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.MacAddress;
import de.schaffbar.core_pos.shared.id.RfidReaderId;
import de.schaffbar.core_pos.support.Json;

@IntegrationTest
class GateKeeperOutAcceptanceTest {

    @Autowired
    private SchaffbarFixture fixture;

    // ------------------------------------------------------------------------
    // init

    @Test
    void init_startsWithoutRelay() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().rfidReader(scenario.macAddress(), GATE_KEEPER_OUT);

        // when
        MvcTestResult result = this.fixture.api().initDevice(scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "STATE": "START",
                    "DEVNAME": "Gate Keeper Out",
                    "DEVUSECASE": "GO",
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
    void card_checksOutWithEntryAndExitTime() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().rfidReader(scenario.macAddress(), GATE_KEEPER_OUT);
        CustomerId customerId = this.fixture.given().customerInTheWorkshop(scenario.rfidTagId());

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "GO",
                    "STATE": "END",
                    "ERROR": "",
                    "ICON": "BYE",
                    "CUSTOMERNAME": "%s"
                }
                """.formatted(this.fixture.then().fullNameOf(customerId)));

        assertThat(Json.read(result, "$.CUSTOMERSTARTSTOP")).matches("\\d{2}:\\d{2} - \\d{2}:\\d{2}");
        assertThat(Json.read(result, "$.UNITS")).matches("\\d+");
    }

    @Test
    void card_switchesTheRunningToolsOff() {
        Scenario scenario = Scenario.randomValues().build();
        MacAddress switchBoxMacAddress = RandomValues.macAddress();

        // given
        RfidReaderId switchBox = this.fixture.given().switchBox(switchBoxMacAddress);
        this.fixture.given().rfidReader(scenario.macAddress(), GATE_KEEPER_OUT);
        this.fixture.given().toolWithSwitchBox(scenario.toolName(), switchBox, CertificationRequirement.YELLOW);

        CustomerId customerId = this.fixture.given().customerInTheWorkshop(scenario.rfidTagId());
        this.fixture.given().toolUsage(scenario.rfidTagId(), switchBoxMacAddress);

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "GO",
                    "STATE": "END",
                    "ERROR": "",
                    "ICON": "BYE",
                    "CUSTOMERNAME": "%s"
                }
                """.formatted(this.fixture.then().fullNameOf(customerId)));

        assertThat(this.fixture.api().getOpenWorkshopSession(customerId)).bodyJson().extractingPath("$.toolUsageSummaries[0].active").isEqualTo(false);
    }

    @Test
    void card_neverCheckedIn_isRefused() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().rfidReader(scenario.macAddress(), GATE_KEEPER_OUT);
        this.fixture.given().rfidTag(scenario.rfidTagId());
        CustomerId customerId = this.fixture.given().customerWithRfidTag(scenario.rfidTagId());

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "test",
                    "STATE": "END",
                    "ERROR": "Du warst nie im Werkstatt",
                    "ICON": "STOP",
                    "CUSTOMERNAME": "%s",
                    "UNITS": "0"
                }
                """.formatted(this.fixture.then().fullNameOf(customerId)));
    }

    @Test
    void card_alreadyCheckedOut_isRefused() {
        Scenario scenario = Scenario.randomValues().build();

        // given – checked out, the session is still open until it is billed
        this.fixture.given().rfidReader(scenario.macAddress(), GATE_KEEPER_OUT);
        CustomerId customerId = this.fixture.given().customerInTheWorkshop(scenario.rfidTagId());
        this.fixture.then().expectSuccess(this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), scenario.macAddress()));

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "test",
                    "STATE": "END",
                    "ERROR": "Zwei mal Pause geht nicht",
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
        this.fixture.given().rfidReader(scenario.macAddress(), GATE_KEEPER_OUT);
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
        this.fixture.given().rfidReader(scenario.macAddress(), GATE_KEEPER_OUT);

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
    void counter_answersError() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().rfidReader(scenario.macAddress(), GATE_KEEPER_OUT);

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToCounterReader(scenario.rfidTagId(), scenario.macAddress());

        // then – this reader does not belong at /counter
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "",
                    "STATE": "END",
                    "ERROR": "Unexpected RFID reader type: GATE_KEEPER_OUT",
                    "CUSTOMERNAME": "Error"
                }
                """);
    }

}
