package de.schaffbar.core_pos.integration.device;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import de.schaffbar.core_pos.integration.IntegrationTest;
import de.schaffbar.core_pos.integration.fixture.Scenario;
import de.schaffbar.core_pos.integration.fixture.SchaffbarFixture;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;

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
                    "ERROR": "Nicht Gefunden",
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
    void counter_unknownReader_isNotHandled() {
        Scenario scenario = Scenario.randomValues().build();

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToCounterReader(scenario.rfidTagId(), scenario.macAddress());

        // then – ApiExceptionHandler covers only domain/, the server answers 500
        assertThat(result).hasFailed();
        assertThat(result.getUnresolvedException()).hasRootCauseInstanceOf(ResourceNotFoundException.class);
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
                    "ERROR": "Nicht Gefunden",
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
    void counter_readerWithoutType_isNotHandled() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().rfidReader(scenario.macAddress());

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToCounterReader(scenario.rfidTagId(), scenario.macAddress());

        // then – the switch over the type fails on null, the server answers 500
        assertThat(result).hasFailed();
        assertThat(result.getUnresolvedException()).hasRootCauseInstanceOf(NullPointerException.class);
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

}
