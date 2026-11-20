package de.schaffbar.core_pos.integration.device;

import static de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType.RFID_TAG_ASSIGNER;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import de.schaffbar.core_pos.integration.IntegrationTest;
import de.schaffbar.core_pos.integration.fixture.Scenario;
import de.schaffbar.core_pos.integration.fixture.SchaffbarFixture;
import de.schaffbar.core_pos.shared.id.CustomerId;

@IntegrationTest
class RfidTagAssignerAcceptanceTest {

    @Autowired
    private SchaffbarFixture fixture;

    // ------------------------------------------------------------------------
    // init

    @Test
    void init_startsWithoutRelay() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().rfidReader(scenario.macAddress(), RFID_TAG_ASSIGNER);

        // when
        MvcTestResult result = this.fixture.api().initDevice(scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "STATE": "START",
                    "DEVNAME": "RFID Tag Assigner",
                    "DEVUSECASE": "C",
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
    void counter_waitingCustomer_getsTheTag() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().rfidReader(scenario.macAddress(), RFID_TAG_ASSIGNER);
        this.fixture.given().rfidTag(scenario.rfidTagId());
        CustomerId customerId = this.fixture.given().customerWaitingForRfidTag();

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToCounterReader(scenario.rfidTagId(), scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "C",
                    "STATE": "END",
                    "ERROR": "",
                    "ICON": "OK",
                    "CUSTOMERNAME": "%s",
                    "RFID": "%s"
                }
                """.formatted(this.fixture.then().fullNameOf(customerId), scenario.rfidTagId().getValue()));

        this.fixture.then().expectRfidTagAssigned(customerId, scenario.rfidTagId());
    }

    @Test
    void counter_assignedTag_showsItsOwner() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().rfidReader(scenario.macAddress(), RFID_TAG_ASSIGNER);
        this.fixture.given().rfidTag(scenario.rfidTagId());
        CustomerId customerId = this.fixture.given().customerWithRfidTag(scenario.rfidTagId());

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToCounterReader(scenario.rfidTagId(), scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "C",
                    "STATE": "END",
                    "ERROR": "",
                    "ICON": "OK",
                    "CUSTOMERNAME": "%s",
                    "RFID": "%s"
                }
                """.formatted(this.fixture.then().fullNameOf(customerId), scenario.rfidTagId().getValue()));
    }

    @Test
    void counter_assignedTagWhileSomebodyWaits_showsItsOwner() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().rfidReader(scenario.macAddress(), RFID_TAG_ASSIGNER);
        this.fixture.given().rfidTag(scenario.rfidTagId());
        CustomerId owner = this.fixture.given().customerWithRfidTag(scenario.rfidTagId());
        CustomerId waitingCustomer = this.fixture.given().customerWaitingForRfidTag();

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToCounterReader(scenario.rfidTagId(), scenario.macAddress());

        // then – the tag stays with its owner, the request keeps waiting
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "C",
                    "STATE": "END",
                    "ERROR": "",
                    "ICON": "OK",
                    "CUSTOMERNAME": "%s",
                    "RFID": "%s"
                }
                """.formatted(this.fixture.then().fullNameOf(owner), scenario.rfidTagId().getValue()));

        this.fixture.then().expectRfidTagAssigned(owner, scenario.rfidTagId());
        this.fixture.then().expectWaitingForRfidTag(waitingCustomer);

        // cleanup
        this.fixture.endWaitingRfidTagAssignment();
    }

    @Test
    void counter_registeredTagWithNobodyWaiting_answersError() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().rfidReader(scenario.macAddress(), RFID_TAG_ASSIGNER);
        this.fixture.given().rfidTag(scenario.rfidTagId());

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToCounterReader(scenario.rfidTagId(), scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "C",
                    "STATE": "END",
                    "ERROR": "RFID nicht zugewiesen",
                    "ICON": null,
                    "CUSTOMERNAME": "Error"
                }
                """);
    }

    @Test
    void counter_unregisteredTag_answersError() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().rfidReader(scenario.macAddress(), RFID_TAG_ASSIGNER);
        CustomerId waitingCustomer = this.fixture.given().customerWaitingForRfidTag();

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToCounterReader(scenario.rfidTagId(), scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "C",
                    "STATE": "END",
                    "ERROR": "RFID Tag unbekannt",
                    "ICON": null,
                    "CUSTOMERNAME": "Error"
                }
                """);
        this.fixture.then().expectWaitingForRfidTag(waitingCustomer);

        // cleanup
        this.fixture.endWaitingRfidTagAssignment();
    }

    @Test
    void counter_unregisteredTagWithNobodyWaiting_answersError() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().rfidReader(scenario.macAddress(), RFID_TAG_ASSIGNER);

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToCounterReader(scenario.rfidTagId(), scenario.macAddress());

        // then
        this.fixture.then().expectDeviceAnswer(result, """
                {
                    "DEVUSECASE": "C",
                    "STATE": "END",
                    "ERROR": "RFID Tag unbekannt",
                    "ICON": null,
                    "CUSTOMERNAME": "Error"
                }
                """);
    }

    // ------------------------------------------------------------------------
    // card

    @Test
    void card_answersUnexpectedError() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().rfidReader(scenario.macAddress(), RFID_TAG_ASSIGNER);
        this.fixture.given().rfidTag(scenario.rfidTagId());
        this.fixture.given().customerWithRfidTag(scenario.rfidTagId());

        // when
        MvcTestResult result = this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), scenario.macAddress());

        // then – this reader does not belong at /card
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
