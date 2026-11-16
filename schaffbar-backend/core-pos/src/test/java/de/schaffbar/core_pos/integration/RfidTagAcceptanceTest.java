package de.schaffbar.core_pos.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import de.schaffbar.core_pos.integration.fixture.Scenario;
import de.schaffbar.core_pos.integration.fixture.SchaffbarFixture;
import de.schaffbar.core_pos.shared.exception.RfidTagAlreadyRegisteredException;
import de.schaffbar.core_pos.shared.exception.RfidTagAssignedToCustomerException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.RfidTagId;

// one test per rule of domain/rfid_tag/README.md
@IntegrationTest
class RfidTagAcceptanceTest {

    @Autowired
    private SchaffbarFixture fixture;

    @Test
    void rt3_anAssignedTagCannotBeDeleted() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        RfidTagId rfidTagId = this.fixture.given().rfidTag(scenario.rfidTagId());
        CustomerId customerId = this.fixture.given().customerWithRfidTag(rfidTagId);

        // when
        MvcTestResult result = this.fixture.api().deleteRfidTag(rfidTagId);

        // then
        this.fixture.then().expectProblem(result, HttpStatus.CONFLICT, RfidTagAssignedToCustomerException.CODE);
        this.fixture.then().expectRfidTag(rfidTagId);

        // when
        MvcTestResult resultUnassign = this.fixture.api().unassignRfidTag(customerId);
        this.fixture.then().expectSuccess(resultUnassign);

        result = this.fixture.api().deleteRfidTag(rfidTagId);

        // then
        this.fixture.then().expectSuccess(result);
        this.fixture.then().expectNoRfidTag(rfidTagId);
    }

    @Test
    void rt5_aTagIsRegisteredOnlyOnce() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        RfidTagId rfidTagId = this.fixture.given().rfidTag(scenario.rfidTagId());

        // when
        MvcTestResult result = this.fixture.api().createRfidTag(rfidTagId);

        // then
        this.fixture.then().expectProblem(result, HttpStatus.CONFLICT, RfidTagAlreadyRegisteredException.CODE);
        this.fixture.then().expectRfidTag(rfidTagId);
    }

}
