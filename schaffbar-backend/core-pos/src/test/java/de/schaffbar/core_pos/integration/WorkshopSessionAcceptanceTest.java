package de.schaffbar.core_pos.integration;

import static de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType.GATE_KEEPER_IN;
import static de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType.GATE_KEEPER_OUT;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import de.schaffbar.core_pos.builder.RandomValues;
import de.schaffbar.core_pos.integration.fixture.Scenario;
import de.schaffbar.core_pos.integration.fixture.SchaffbarFixture;
import de.schaffbar.core_pos.shared.exception.NoOpenWorkshopSessionException;
import de.schaffbar.core_pos.shared.exception.RunningWorkshopUsageExistsForSessionException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.MacAddress;

@IntegrationTest
class WorkshopSessionAcceptanceTest {

    @Autowired
    private SchaffbarFixture fixture;

    @Test
    void ws3_theSessionOfAnUnknownCustomerCannotBeClosed() {
        // when
        MvcTestResult result = this.fixture.api().closeWorkshopSession(CustomerId.random());

        // then
        this.fixture.then().expectProblem(result, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    @Test
    void ws4_onlyAnOpenSessionCanBeClosed() {
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
        MvcTestResult result = this.fixture.api().closeWorkshopSession(customerId);

        // then
        this.fixture.then().expectSuccess(result);

        // when – the session is paid
        result = this.fixture.api().closeWorkshopSession(customerId);

        // then
        this.fixture.then().expectProblem(result, HttpStatus.CONFLICT, NoOpenWorkshopSessionException.CODE);
    }

    // the tool side of WS-5 cannot be reached through the API: checking out switches the tools off (TU-4)
    @Test
    void ws5_aSessionCannotBeClosedWhileThePersonIsInTheWorkshop() {
        Scenario scenario = Scenario.randomValues().build();
        MacAddress gateKeeperIn = scenario.macAddress();
        MacAddress gateKeeperOut = RandomValues.macAddress();

        // given
        this.fixture.given().rfidReader(gateKeeperIn, GATE_KEEPER_IN);
        this.fixture.given().rfidReader(gateKeeperOut, GATE_KEEPER_OUT);
        this.fixture.given().rfidTag(scenario.rfidTagId());
        CustomerId customerId = this.fixture.given().customerInTheWorkshop(scenario.rfidTagId(), gateKeeperIn);

        // when
        MvcTestResult result = this.fixture.api().closeWorkshopSession(customerId);

        // then
        this.fixture.then().expectProblem(result, HttpStatus.CONFLICT, RunningWorkshopUsageExistsForSessionException.CODE);
        this.fixture.then().expectOpenWorkshopSession(customerId);

        // when
        this.fixture.api().holdRfidTagToReader(scenario.rfidTagId(), gateKeeperOut);
        result = this.fixture.api().closeWorkshopSession(customerId);

        // then
        this.fixture.then().expectSuccess(result);
        this.fixture.then().expectNoOpenWorkshopSession(customerId);
    }

}
