package de.schaffbar.core_pos.integration.device;

import static de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType.GATE_KEEPER_OUT;

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
import de.schaffbar.core_pos.shared.id.ToolId;

@IntegrationTest
class GateKeeperOutAcceptanceTest {

    @Autowired
    private SchaffbarFixture fixture;

    // ------------------------------------------------------------------------
    // init

    @Test
    void init_registeredReader_startsWithoutRelay() {
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
    void card_inTheWorkshop_checksOutWithEntryAndExitTime() {
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
                """.formatted(this.fixture.fullNameOf(customerId)));

        this.fixture.then().expectEntryAndExitTimeShown(result);
        this.fixture.then().expectCheckedOut(customerId);
    }

    // WU-5
    @Test
    void card_withARunningTool_switchesItOff() {
        Scenario scenario = Scenario.randomValues().build();
        MacAddress switchBoxMacAddress = RandomValues.macAddress();

        // given
        RfidReaderId switchBox = this.fixture.given().switchBox(switchBoxMacAddress);
        this.fixture.given().rfidReader(scenario.macAddress(), GATE_KEEPER_OUT);
        ToolId toolId = this.fixture.given().toolWithSwitchBox(scenario.toolName(), switchBox, CertificationRequirement.YELLOW);

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
                """.formatted(this.fixture.fullNameOf(customerId)));

        this.fixture.then().expectToolStopped(customerId, toolId);
        this.fixture.then().expectCheckedOut(customerId);
    }

    // WU-9
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
                """.formatted(this.fixture.fullNameOf(customerId)));
    }

    // WU-2
    @Test
    void card_alreadyCheckedOut_isRefused() {
        Scenario scenario = Scenario.randomValues().build();

        // given – checked out, the session is still open until it is billed
        this.fixture.given().rfidReader(scenario.macAddress(), GATE_KEEPER_OUT);
        CustomerId customerId = this.fixture.given().customerInTheWorkshop(scenario.rfidTagId());
        this.fixture.given().customerLeftTheWorkshop(scenario.rfidTagId(), scenario.macAddress());

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
                """.formatted(this.fixture.fullNameOf(customerId)));
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
    void counter_anyTag_isTheWrongReader() {
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
                    "ERROR": "Falsches Lesegerät",
                    "CUSTOMERNAME": "Error"
                }
                """);
    }

}
