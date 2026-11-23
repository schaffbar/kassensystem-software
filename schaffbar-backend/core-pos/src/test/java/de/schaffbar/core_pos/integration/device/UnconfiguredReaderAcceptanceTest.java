package de.schaffbar.core_pos.integration.device;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import de.schaffbar.core_pos.integration.IntegrationTest;
import de.schaffbar.core_pos.integration.fixture.Scenario;
import de.schaffbar.core_pos.integration.fixture.SchaffbarFixture;

@IntegrationTest
class UnconfiguredReaderAcceptanceTest {

    @Autowired
    private SchaffbarFixture fixture;

    // ------------------------------------------------------------------------
    // unknown reader

    @Test
    void init_unknownReader_isRegisteredWithoutType() {
        Scenario scenario = Scenario.randomValues().build();

        // when
        MvcTestResult result = this.fixture.api().initDevice(scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "STATE": "ERROR",
                    "DEVNAME": "ERROR",
                    "DEVUSECASE": "ERROR",
                    "ERROR": "Lesegerät ohne Typ",
                    "STARTHTTP": "",
                    "DEVIP": "",
                    "SWITCHON": "",
                    "SWITCHOFF": "",
                    "TERMINAL": ""
                }
                """);
        this.fixture.then().expectRfidReaderCount(scenario.macAddress(), 1);
    }

    @Test
    void counter_unknownReader_isNotRecognized() {
        Scenario scenario = Scenario.randomValues().build();

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToCounterReader(scenario.rfidTagId(), scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "",
                    "STATE": "END",
                    "ERROR": "RFID Reader nicht erkannt",
                    "ICON": null,
                    "CUSTOMERNAME": "Error"
                }
                """);
    }

    @Test
    void card_unknownReader_isNotRecognized() {
        Scenario scenario = Scenario.randomValues().build();

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "test",
                    "STATE": "END",
                    "ERROR": "RFID Reader nicht erkannt",
                    "ICON": "NOREG",
                    "CUSTOMERNAME": null,
                    "UNITS": "0"
                }
                """);
    }

    // ------------------------------------------------------------------------
    // reader without type

    @Test
    void init_readerWithoutType_answersError() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().rfidReader(scenario.macAddress());

        // when
        MvcTestResult result = this.fixture.api().initDevice(scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "STATE": "ERROR",
                    "DEVNAME": "ERROR",
                    "DEVUSECASE": "ERROR",
                    "ERROR": "Lesegerät ohne Typ",
                    "STARTHTTP": "",
                    "DEVIP": "",
                    "SWITCHON": "",
                    "SWITCHOFF": "",
                    "TERMINAL": ""
                }
                """);
        this.fixture.then().expectRfidReaderCount(scenario.macAddress(), 1);
    }

    @Test
    void counter_readerWithoutType_answersError() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().rfidReader(scenario.macAddress());

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToCounterReader(scenario.rfidTagId(), scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "",
                    "STATE": "END",
                    "ERROR": "Lesegerät ohne Typ",
                    "ICON": null,
                    "CUSTOMERNAME": "Error"
                }
                """);
    }

    @Test
    void card_readerWithoutType_answersError() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().rfidReader(scenario.macAddress());
        this.fixture.given().rfidTag(scenario.rfidTagId());
        this.fixture.given().customerWithRfidTag(scenario.rfidTagId());

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "test",
                    "STATE": "END",
                    "ERROR": "Lesegerät ohne Typ",
                    "ICON": "NOREG",
                    "CUSTOMERNAME": null,
                    "UNITS": "0"
                }
                """);
    }

    // ------------------------------------------------------------------------
    // broken request – the device gets its answer, never Spring's error page

    @Test
    void init_brokenMacAddress_answersUnexpectedError() {
        // given – dashes instead of colons (R-7)
        String brokenMacAddress = "34-CD-B0-D3-A9-30";

        // when
        MvcTestResult result = this.fixture.api().initDevice(brokenMacAddress);

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "STATE": "ERROR",
                    "DEVNAME": "ERROR",
                    "DEVUSECASE": "ERROR",
                    "ERROR": "Unerwarteter Fehler",
                    "STARTHTTP": "",
                    "DEVIP": "",
                    "SWITCHON": "",
                    "SWITCHOFF": "",
                    "TERMINAL": ""
                }
                """);
    }

    @Test
    void counter_brokenMacAddress_answersUnexpectedError() {
        Scenario scenario = Scenario.randomValues().build();

        // given – dashes instead of colons (R-7)
        String brokenMacAddress = "34-CD-B0-D3-A9-30";

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToCounterReader(scenario.rfidTagId().getValue(), brokenMacAddress);

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "",
                    "STATE": "END",
                    "ERROR": "Unerwarteter Fehler",
                    "ICON": null,
                    "CUSTOMERNAME": "Error"
                }
                """);
    }

    @Test
    void card_brokenMacAddress_answersUnexpectedError() {
        Scenario scenario = Scenario.randomValues().build();

        // given – dashes instead of colons (R-7)
        String brokenMacAddress = "34-CD-B0-D3-A9-30";

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToReader(scenario.rfidTagId().getValue(), brokenMacAddress);

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

}
