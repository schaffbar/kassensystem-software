package de.schaffbar.core_pos.integration.device;

import static de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType.RFID_TAG_REGISTER;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import de.schaffbar.core_pos.integration.IntegrationTest;
import de.schaffbar.core_pos.integration.fixture.Scenario;
import de.schaffbar.core_pos.integration.fixture.SchaffbarFixture;

@IntegrationTest
class RfidTagRegisterAcceptanceTest {

    @Autowired
    private SchaffbarFixture fixture;

    // ------------------------------------------------------------------------
    // init

    @Test
    void init_startsWithoutRelay() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().rfidReader(scenario.macAddress(), RFID_TAG_REGISTER);

        // when
        MvcTestResult result = this.fixture.api().initDevice(scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "STATE": "START",
                    "DEVNAME": "RFID Tag Register",
                    "DEVUSECASE": "A",
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
    // counter

    @Test
    void counter_newTag_registersIt() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().rfidReader(scenario.macAddress(), RFID_TAG_REGISTER);

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToCounterReader(scenario.rfidTagId(), scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "A",
                    "STATE": "END",
                    "ERROR": "",
                    "ICON": "OK",
                    "CUSTOMERNAME": null,
                    "RFID": null
                }
                """);

        this.fixture.then().expectRfidTag(scenario.rfidTagId());
    }

    @Test
    void counter_registeredTag_answersError() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().rfidReader(scenario.macAddress(), RFID_TAG_REGISTER);
        this.fixture.given().rfidTag(scenario.rfidTagId());

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToCounterReader(scenario.rfidTagId(), scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "A",
                    "STATE": "END",
                    "ERROR": "RFID bereits vorhanden!",
                    "ICON": null,
                    "CUSTOMERNAME": "Error"
                }
                """);
    }

    @Test
    void counter_blankTag_answersUnexpectedError() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().rfidReader(scenario.macAddress(), RFID_TAG_REGISTER);

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToCounterReader("", scenario.macAddress().getValue());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "A",
                    "STATE": "END",
                    "ERROR": "Unerwarteter Fehler",
                    "ICON": null,
                    "CUSTOMERNAME": "Error"
                }
                """);
    }

    // ------------------------------------------------------------------------
    // card

    @Test
    void card_isTheWrongReader() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().rfidReader(scenario.macAddress(), RFID_TAG_REGISTER);
        this.fixture.given().rfidTag(scenario.rfidTagId());
        this.fixture.given().customerWithRfidTag(scenario.rfidTagId());

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), scenario.macAddress());

        // then – this reader does not belong at /card
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "test",
                    "STATE": "END",
                    "ERROR": "Falsches Lesegerät",
                    "ICON": "NOREG",
                    "CUSTOMERNAME": null,
                    "UNITS": "0"
                }
                """);
    }

}
