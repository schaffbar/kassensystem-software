package de.schaffbar.core_pos.integration.device;

import static de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType.SWITCH_BOX;
import static de.schaffbar.core_pos.domain.tool.WlanRelaisType.SHELLY_1;
import static de.schaffbar.core_pos.domain.tool.WlanRelaisType.SHELLY_PRO;

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

@IntegrationTest
class SwitchBoxAcceptanceTest {

    @Autowired
    private SchaffbarFixture fixture;

    // ------------------------------------------------------------------------
    // init

    @Test
    void init_toolWithShelly1_getsTheRelayCommands() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        RfidReaderId switchBox = this.fixture.given().switchBox(scenario.macAddress());
        this.fixture.given().toolWithSwitchBoxAndWlanRelais(scenario.toolName(), switchBox, scenario.ipAddress(), SHELLY_1);

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
                    "SWITCHON": "/relay/0?turn=on",
                    "SWITCHOFF": "/relay/0?turn=off",
                    "TERMINAL": ""
                }
                """.formatted(scenario.ipAddress().getValue()));
    }

    @Test
    void init_toolWithShellyPro_getsTheRelayCommands() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        RfidReaderId switchBox = this.fixture.given().switchBox(scenario.macAddress());
        this.fixture.given().toolWithSwitchBoxAndWlanRelais(scenario.toolName(), switchBox, scenario.ipAddress(), SHELLY_PRO);

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
                    "SWITCHON": "/rpc/Switch.Set?id=0&on=true",
                    "SWITCHOFF": "/rpc/Switch.Set?id=0&on=false",
                    "TERMINAL": ""
                }
                """.formatted(scenario.ipAddress().getValue()));
    }

    @Test
    void init_toolWithoutWlanRelais_answersError() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        RfidReaderId switchBox = this.fixture.given().switchBox(scenario.macAddress());
        this.fixture.given().toolWithSwitchBox(scenario.toolName(), switchBox, CertificationRequirement.YELLOW);

        // when
        MvcTestResult result = this.fixture.api().initDevice(scenario.macAddress());

        // then – every tool is switched over its WLAN relais (T-23)
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "STATE": "ERROR",
                    "DEVNAME": "Switch Box",
                    "DEVUSECASE": "S",
                    "ERROR": "Kein WLAN-Relais",
                    "STARTHTTP": "",
                    "DEVIP": "",
                    "SWITCHON": "",
                    "SWITCHOFF": "",
                    "TERMINAL": ""
                }
                """);
    }

    @Test
    void init_withoutTool_answersError() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().switchBox(scenario.macAddress());

        // when
        MvcTestResult result = this.fixture.api().initDevice(scenario.macAddress());

        // then – a switch box without tool has nothing to switch (R-8)
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "STATE": "ERROR",
                    "DEVNAME": "Switch Box",
                    "DEVUSECASE": "S",
                    "ERROR": "Kein Werkzeug",
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
    void card_toolNotRunning_switchesItOn() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        RfidReaderId switchBox = this.fixture.given().switchBox(scenario.macAddress());
        ToolId toolId = this.fixture.given().toolWithSwitchBox(scenario.toolName(), switchBox, CertificationRequirement.YELLOW);
        CustomerId customerId = this.fixture.given().customerInTheWorkshop(scenario.rfidTagId());

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "S",
                    "STATE": "WORKING",
                    "ERROR": "",
                    "ICON": "",
                    "CUSTOMERNAME": "%s",
                    "CUSTOMERSTARTSTOP": "%s",
                    "UNITS": ""
                }
                """.formatted(this.fixture.fullNameOf(customerId), scenario.toolName()));
        this.fixture.then().expectToolRunning(customerId, toolId);
    }

    // TU-11
    @Test
    void card_toolRunning_switchesItOff() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        RfidReaderId switchBox = this.fixture.given().switchBox(scenario.macAddress());
        ToolId toolId = this.fixture.given().toolWithSwitchBox(scenario.toolName(), switchBox, CertificationRequirement.YELLOW);
        CustomerId customerId = this.fixture.given().customerInTheWorkshop(scenario.rfidTagId());
        this.fixture.given().toolUsage(scenario.rfidTagId(), scenario.macAddress());

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "S",
                    "STATE": "END",
                    "ERROR": "",
                    "ICON": "BYE",
                    "CUSTOMERNAME": "%s",
                    "CUSTOMERSTARTSTOP": "%s",
                    "UNITS": ""
                }
                """.formatted(this.fixture.fullNameOf(customerId), scenario.toolName()));
        this.fixture.then().expectToolStopped(customerId, toolId);
    }

    // TU-10
    @Test
    void card_redToolWithCertification_switchesItOn() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        RfidReaderId switchBox = this.fixture.given().switchBox(scenario.macAddress());
        ToolId toolId = this.fixture.given().toolWithSwitchBox(scenario.toolName(), switchBox, CertificationRequirement.RED);
        CustomerId customerId = this.fixture.given().customerInTheWorkshop(scenario.rfidTagId());
        CustomerId instructorId = this.fixture.given().instructor(toolId);
        this.fixture.given().toolCertification(customerId, toolId, instructorId);

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "S",
                    "STATE": "WORKING",
                    "ERROR": "",
                    "ICON": "",
                    "CUSTOMERNAME": "%s",
                    "CUSTOMERSTARTSTOP": "%s",
                    "UNITS": ""
                }
                """.formatted(this.fixture.fullNameOf(customerId), scenario.toolName()));
        this.fixture.then().expectToolRunning(customerId, toolId);
    }

    // TU-2
    @Test
    void card_notInTheWorkshop_isRefused() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        RfidReaderId switchBox = this.fixture.given().switchBox(scenario.macAddress());
        ToolId toolId = this.fixture.given().toolWithSwitchBox(scenario.toolName(), switchBox, CertificationRequirement.YELLOW);
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
                """.formatted(this.fixture.fullNameOf(customerId)));
        this.fixture.then().expectToolNotRunningFor(customerId, toolId);
    }

    // TU-10
    @Test
    void card_redToolWithoutCertification_isRefused() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        RfidReaderId switchBox = this.fixture.given().switchBox(scenario.macAddress());
        ToolId toolId = this.fixture.given().toolWithSwitchBox(scenario.toolName(), switchBox, CertificationRequirement.RED);
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
                """.formatted(this.fixture.fullNameOf(customerId)));
        this.fixture.then().expectToolNotRunningFor(customerId, toolId);
    }

    // TU-10 – a paused certification counts like none
    @Test
    void card_redToolWithPausedCertification_isRefused() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        RfidReaderId switchBox = this.fixture.given().switchBox(scenario.macAddress());
        ToolId toolId = this.fixture.given().toolWithSwitchBox(scenario.toolName(), switchBox, CertificationRequirement.RED);
        CustomerId customerId = this.fixture.given().customerInTheWorkshop(scenario.rfidTagId());
        CustomerId instructorId = this.fixture.given().instructor(toolId);
        this.fixture.given().pausedToolCertification(customerId, toolId, instructorId);

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
                """.formatted(this.fixture.fullNameOf(customerId)));
        this.fixture.then().expectToolNotRunningFor(customerId, toolId);
    }

    // TU-1
    @Test
    void card_thirdToolAtTheSameTime_isRefused() {
        Scenario scenario = Scenario.randomValues().build();
        MacAddress firstSwitchBoxMacAddress = RandomValues.macAddress();
        MacAddress secondSwitchBoxMacAddress = RandomValues.macAddress();

        // given
        RfidReaderId firstSwitchBox = this.fixture.given().switchBox(firstSwitchBoxMacAddress);
        RfidReaderId secondSwitchBox = this.fixture.given().switchBox(secondSwitchBoxMacAddress);
        RfidReaderId thirdSwitchBox = this.fixture.given().switchBox(scenario.macAddress());

        this.fixture.given().toolWithSwitchBox(RandomValues.toolName(), firstSwitchBox, CertificationRequirement.YELLOW);
        this.fixture.given().toolWithSwitchBox(RandomValues.toolName(), secondSwitchBox, CertificationRequirement.YELLOW);
        ToolId toolId = this.fixture.given().toolWithSwitchBox(scenario.toolName(), thirdSwitchBox, CertificationRequirement.YELLOW);

        CustomerId customerId = this.fixture.given().customerInTheWorkshop(scenario.rfidTagId());
        this.fixture.given().toolUsage(scenario.rfidTagId(), firstSwitchBoxMacAddress);
        this.fixture.given().toolUsage(scenario.rfidTagId(), secondSwitchBoxMacAddress);

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), scenario.macAddress());

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
                """.formatted(this.fixture.fullNameOf(customerId)));
        this.fixture.then().expectToolNotRunningFor(customerId, toolId);
    }

    // TU-9
    @Test
    void card_toolUsedByAnotherCustomer_isRefused() {
        Scenario scenario = Scenario.randomValues().build();
        RfidTagId otherRfidTagId = RandomValues.rfidTagId();

        // given
        RfidReaderId switchBox = this.fixture.given().switchBox(scenario.macAddress());
        ToolId toolId = this.fixture.given().toolWithSwitchBox(scenario.toolName(), switchBox, CertificationRequirement.YELLOW);
        CustomerId customerId = this.fixture.given().customerInTheWorkshop(scenario.rfidTagId());

        this.fixture.given().customerInTheWorkshop(otherRfidTagId);
        this.fixture.given().toolUsage(otherRfidTagId, scenario.macAddress());

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "test",
                    "STATE": "END",
                    "ERROR": "Werkzeug belegt",
                    "ICON": "STOP",
                    "CUSTOMERNAME": "%s",
                    "UNITS": "0"
                }
                """.formatted(this.fixture.fullNameOf(customerId)));
        this.fixture.then().expectToolNotRunningFor(customerId, toolId);
    }

    @Test
    void card_switchBoxWithoutTool_isRefused() {
        Scenario scenario = Scenario.randomValues().build();

        // given – the tool was taken from the switch box after it started (R-8)
        this.fixture.given().switchBox(scenario.macAddress());
        CustomerId customerId = this.fixture.given().customerInTheWorkshop(scenario.rfidTagId());

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "test",
                    "STATE": "END",
                    "ERROR": "Kein Werkzeug",
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
    void counter_anyTag_isTheWrongReader() {
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
                    "ERROR": "Falsches Lesegerät",
                    "CUSTOMERNAME": "Error"
                }
                """);
    }

}
