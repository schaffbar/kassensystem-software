package de.schaffbar.core_pos.integration.device;

import static de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType.SWITCH_BOX;

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
import de.schaffbar.core_pos.shared.id.RfidTagId;
import de.schaffbar.core_pos.shared.id.ToolId;
import de.schaffbar.core_pos.support.Json;

@IntegrationTest
class SwitchBoxAcceptanceTest {

    @Autowired
    private SchaffbarFixture fixture;

    // ------------------------------------------------------------------------
    // init

    @Test
    void init_toolWithWlanRelais_getsTheRelayCommands() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        RfidReaderId switchBox = this.fixture.given().switchBox(scenario.macAddress());
        ToolId toolId = this.fixture.given().toolWithSwitchBoxAndWlanRelais(scenario.toolName(), switchBox, scenario.ipAddress());
        MvcTestResult tool = this.fixture.api().getTool(toolId);

        // when
        MvcTestResult result = this.fixture.api().initDevice(scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "STATE": "START",
                    "DEVNAME": "Switch Box",
                    "DEVUSECASE": "S",
                    "ERROR": "",
                    "STARTHTTP": "http://",
                    "DEVIP": "%s",
                    "SWITCHON": "/relay/0?%s",
                    "SWITCHOFF": "/relay/0?%s",
                    "TERMINAL": ""
                }
                """.formatted(scenario.ipAddress().getValue(), Json.read(tool, "$.onCommand"), Json.read(tool, "$.offCommand")));
    }

    // TODO: fix it - error when no WLAN relais
    @Test
    void init_toolWithoutWlanRelais_getsNoAddress() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        RfidReaderId switchBox = this.fixture.given().switchBox(scenario.macAddress());
        this.fixture.given().toolWithSwitchBox(scenario.toolName(), switchBox, CertificationRequirement.YELLOW);

        // when
        MvcTestResult result = this.fixture.api().initDevice(scenario.macAddress());

        // then – the commands are only the prefix
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "STATE": "START",
                    "DEVNAME": "Switch Box",
                    "DEVUSECASE": "S",
                    "ERROR": "",
                    "STARTHTTP": "http://",
                    "DEVIP": "",
                    "SWITCHON": "/relay/0?",
                    "SWITCHOFF": "/relay/0?",
                    "TERMINAL": ""
                }
                """);
    }

    // TODO: fix it - error when no tool assigned
    @Test
    void init_withoutTool_startsWithoutRelay() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().switchBox(scenario.macAddress());

        // when
        MvcTestResult result = this.fixture.api().initDevice(scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "STATE": "START",
                    "DEVNAME": "Switch Box",
                    "DEVUSECASE": "S",
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
    void card_switchesTheToolOnAndOff() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        RfidReaderId switchBox = this.fixture.given().switchBox(scenario.macAddress());
        this.fixture.given().toolWithSwitchBox(scenario.toolName(), switchBox, CertificationRequirement.YELLOW);
        CustomerId customerId = this.fixture.given().customerInTheWorkshop(scenario.rfidTagId());

        // when
        MvcTestResult switchedOn = this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(switchedOn, """
                {
                    "DEVUSECASE": "S",
                    "STATE": "WORKING",
                    "ERROR": "",
                    "ICON": "",
                    "CUSTOMERNAME": "%s",
                    "CUSTOMERSTARTSTOP": "%s",
                    "UNITS": ""
                }
                """.formatted(this.fixture.then().fullNameOf(customerId), scenario.toolName()));

        // when
        MvcTestResult switchedOff = this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(switchedOff, """
                {
                    "DEVUSECASE": "S",
                    "STATE": "END",
                    "ERROR": "",
                    "ICON": "BYE",
                    "CUSTOMERNAME": "%s",
                    "CUSTOMERSTARTSTOP": "%s",
                    "UNITS": ""
                }
                """.formatted(this.fixture.then().fullNameOf(customerId), scenario.toolName()));
    }

    @Test
    void card_notInTheWorkshop_isRefused() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        RfidReaderId switchBox = this.fixture.given().switchBox(scenario.macAddress());
        this.fixture.given().toolWithSwitchBox(scenario.toolName(), switchBox, CertificationRequirement.YELLOW);
        this.fixture.given().rfidTag(scenario.rfidTagId());
        CustomerId customerId = this.fixture.given().customerWithRfidTag(scenario.rfidTagId());

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "test",
                    "STATE": "END",
                    "ERROR": "Du bist nicht in der Werkstatt",
                    "ICON": "STOP",
                    "CUSTOMERNAME": "%s",
                    "UNITS": "0"
                }
                """.formatted(this.fixture.then().fullNameOf(customerId)));
    }

    @Test
    void card_redToolWithoutCertification_isRefused() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        RfidReaderId switchBox = this.fixture.given().switchBox(scenario.macAddress());
        this.fixture.given().toolWithSwitchBox(scenario.toolName(), switchBox, CertificationRequirement.RED);
        CustomerId customerId = this.fixture.given().customerInTheWorkshop(scenario.rfidTagId());

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "test",
                    "STATE": "END",
                    "ERROR": "Keine Einweisung",
                    "ICON": "STOP",
                    "CUSTOMERNAME": "%s",
                    "UNITS": "0"
                }
                """.formatted(this.fixture.then().fullNameOf(customerId)));
    }

    @Test
    void card_thirdToolAtTheSameTime_isRefused() {
        RfidTagId rfidTagId = RandomValues.rfidTagId();
        MacAddress firstSwitchBoxMacAddress = RandomValues.macAddress();
        MacAddress secondSwitchBoxMacAddress = RandomValues.macAddress();
        MacAddress thirdSwitchBoxMacAddress = RandomValues.macAddress();

        // given
        RfidReaderId firstSwitchBox = this.fixture.given().switchBox(firstSwitchBoxMacAddress);
        RfidReaderId secondSwitchBox = this.fixture.given().switchBox(secondSwitchBoxMacAddress);
        RfidReaderId thirdSwitchBox = this.fixture.given().switchBox(thirdSwitchBoxMacAddress);

        this.fixture.given().toolWithSwitchBox(RandomValues.toolName(), firstSwitchBox, CertificationRequirement.YELLOW);
        this.fixture.given().toolWithSwitchBox(RandomValues.toolName(), secondSwitchBox, CertificationRequirement.YELLOW);
        this.fixture.given().toolWithSwitchBox(RandomValues.toolName(), thirdSwitchBox, CertificationRequirement.YELLOW);

        CustomerId customerId = this.fixture.given().customerInTheWorkshop(rfidTagId);
        this.fixture.given().toolUsage(rfidTagId, firstSwitchBoxMacAddress);
        this.fixture.given().toolUsage(rfidTagId, secondSwitchBoxMacAddress);

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToReader(rfidTagId, thirdSwitchBoxMacAddress);

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "test",
                    "STATE": "END",
                    "ERROR": "Max. Werkzeuge erreicht",
                    "ICON": "STOP",
                    "CUSTOMERNAME": "%s",
                    "UNITS": "0"
                }
                """.formatted(this.fixture.then().fullNameOf(customerId)));
    }

    // TODO: business logik - tool already in use
    @Test
    void card_toolUsedByAnotherCustomer_answersUnexpectedError() {
        Scenario scenario = Scenario.randomValues().build();
        RfidTagId otherRfidTagId = RandomValues.rfidTagId();

        // given
        RfidReaderId switchBox = this.fixture.given().switchBox(scenario.macAddress());
        this.fixture.given().toolWithSwitchBox(scenario.toolName(), switchBox, CertificationRequirement.YELLOW);
        this.fixture.given().customerInTheWorkshop(scenario.rfidTagId());

        this.fixture.given().customerInTheWorkshop(otherRfidTagId);
        this.fixture.given().toolUsage(otherRfidTagId, scenario.macAddress());

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), scenario.macAddress());

        // then – no message of its own today (TOOL_ALREADY_IN_USE_BY_ANOTHER_CUSTOMER)
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "test",
                    "STATE": "END",
                    "ERROR": "Unerwarteter Fehler",
                    "ICON": "NOREG",
                    "CUSTOMERNAME": null,
                    "UNITS": "0"
                }
                """);
    }

    // TODO: business logik - no tool assigned
    @Test
    void card_switchboxWithoutTool_answersUnexpectedError() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().switchBox(scenario.macAddress());
        this.fixture.given().customerInTheWorkshop(scenario.rfidTagId());

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "test",
                    "STATE": "END",
                    "ERROR": "Unerwarteter Fehler",
                    "ICON": "NOREG",
                    "CUSTOMERNAME": null,
                    "UNITS": "0"
                }
                """);
    }

    @Test
    void card_tagWithoutCustomer_isNotRecognized() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().rfidReader(scenario.macAddress(), SWITCH_BOX);
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
        this.fixture.given().rfidReader(scenario.macAddress(), SWITCH_BOX);

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
        this.fixture.given().rfidReader(scenario.macAddress(), SWITCH_BOX);

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToCounterReader(scenario.rfidTagId(), scenario.macAddress());

        // then – this reader does not belong at /counter
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "",
                    "STATE": "END",
                    "ERROR": "Unexpected RFID reader type: SWITCH_BOX",
                    "CUSTOMERNAME": "Error"
                }
                """);
    }

}
