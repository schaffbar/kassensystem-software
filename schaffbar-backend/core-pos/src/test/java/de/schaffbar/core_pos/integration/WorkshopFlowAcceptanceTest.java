package de.schaffbar.core_pos.integration;

import static de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType.GATE_KEEPER_IN;
import static de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType.GATE_KEEPER_OUT;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import de.schaffbar.core_pos.builder.RandomValues;
import de.schaffbar.core_pos.integration.fixture.Scenario;
import de.schaffbar.core_pos.integration.fixture.SchaffbarFixture;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.MacAddress;
import de.schaffbar.core_pos.shared.id.RfidReaderId;
import de.schaffbar.core_pos.shared.id.ToolId;

// the visit of one person through all aggregates – the answers of the devices are checked in integration/device/
@IntegrationTest
class WorkshopFlowAcceptanceTest {

    @Autowired
    private SchaffbarFixture fixture;

    @Test
    void aVisitFromCheckInToBilling() {
        Scenario scenario = Scenario.randomValues().build();
        MacAddress gateKeeperIn = scenario.macAddress();
        MacAddress gateKeeperOut = RandomValues.macAddress();
        MacAddress switchBoxMacAddress = RandomValues.macAddress();

        // given
        this.fixture.given().rfidReader(gateKeeperIn, GATE_KEEPER_IN);
        this.fixture.given().rfidReader(gateKeeperOut, GATE_KEEPER_OUT);
        RfidReaderId switchBox = this.fixture.given().switchBox(switchBoxMacAddress);
        ToolId toolId = this.fixture.given().toolWithSwitchBox(scenario.toolName(), switchBox);
        this.fixture.given().rfidTag(scenario.rfidTagId());
        CustomerId customerId = this.fixture.given().customerWithRfidTag(scenario.rfidTagId());

        // when – the first check-in starts the session
        this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), gateKeeperIn);

        // then
        String firstSessionId = this.fixture.then().expectOpenWorkshopSession(customerId);
        this.fixture.then().expectInTheWorkshop(customerId);

        // when
        this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), switchBoxMacAddress);

        // then
        this.fixture.then().expectToolRunning(customerId, toolId);

        // when
        this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), switchBoxMacAddress);
        this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), gateKeeperOut);

        // then
        this.fixture.then().expectToolStopped(customerId, toolId);
        this.fixture.then().expectCheckedOut(customerId);

        // when – billed at the counter
        MvcTestResult closed = this.fixture.api().closeWorkshopSession(customerId);

        // then
        this.fixture.then().expectSuccess(closed);
        this.fixture.then().expectNoOpenWorkshopSession(customerId);

        // when – the next visit
        this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), gateKeeperIn);

        // then – a new session
        String secondSessionId = this.fixture.then().expectOpenWorkshopSession(customerId);
        assertThat(secondSessionId).isNotEqualTo(firstSessionId);
    }

}
