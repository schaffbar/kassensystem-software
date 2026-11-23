package de.schaffbar.core_pos.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import de.schaffbar.core_pos.builder.RandomValues;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentType;
import de.schaffbar.core_pos.integration.fixture.Scenario;
import de.schaffbar.core_pos.integration.fixture.SchaffbarFixture;
import de.schaffbar.core_pos.shared.exception.CustomerHasRfidTagAssignmentException;
import de.schaffbar.core_pos.shared.exception.NoRfidTagAssignedException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.RfidTagAlreadyAssignedException;
import de.schaffbar.core_pos.shared.exception.RfidTagAssignmentAlreadyWaitingException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.RfidTagId;

// one test per rule of domain/rfid_tag_assignment/README.md; each ends without a waiting request (TA-3)
@IntegrationTest
class RfidTagAssignmentAcceptanceTest {

    @Autowired
    private SchaffbarFixture fixture;

    @Test
    void ta1_aCustomerHasAtMostOneAssignment() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        RfidTagId rfidTagId = this.fixture.given().rfidTag(scenario.rfidTagId());
        CustomerId customerId = this.fixture.given().customerWithRfidTag(rfidTagId);

        // when
        MvcTestResult result = this.fixture.api().requestRfidTagAssignment(customerId, RfidTagAssignmentType.TEMPORARY);

        // then
        this.fixture.then().expectProblem(result, HttpStatus.CONFLICT, CustomerHasRfidTagAssignmentException.CODE);
        this.fixture.then().expectRfidTagAssigned(customerId, rfidTagId);

        // when
        this.fixture.then().expectSuccess(this.fixture.api().unassignRfidTag(customerId));
        result = this.fixture.api().requestRfidTagAssignment(customerId, RfidTagAssignmentType.TEMPORARY);

        // then
        this.fixture.then().expectSuccess(result);
        this.fixture.then().expectWaitingForRfidTag(customerId);

        // cleanup
        this.fixture.endWaitingRfidTagAssignment();
    }

    @Test
    void ta2_aTagIsAssignedToOneCustomerOnly() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        RfidTagId rfidTagId = this.fixture.given().rfidTag(scenario.rfidTagId());
        CustomerId customerWithTag = this.fixture.given().customerWithRfidTag(rfidTagId);
        CustomerId waitingCustomer = this.fixture.given().customerWaitingForRfidTag();

        // when
        MvcTestResult result = this.fixture.api().assignRfidTag(rfidTagId);

        // then
        this.fixture.then().expectProblem(result, HttpStatus.CONFLICT, RfidTagAlreadyAssignedException.CODE);
        this.fixture.then().expectRfidTagAssigned(customerWithTag, rfidTagId);
        this.fixture.then().expectWaitingForRfidTag(waitingCustomer);

        // when
        RfidTagId otherRfidTagId = this.fixture.given().rfidTag(RandomValues.rfidTagId());
        result = this.fixture.api().assignRfidTag(otherRfidTagId);

        // then
        this.fixture.then().expectSuccess(result);
        this.fixture.then().expectRfidTagAssigned(waitingCustomer, otherRfidTagId);
    }

    @Test
    void ta3_onlyOneRequestWaitsAtATime() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        RfidTagId rfidTagId = this.fixture.given().rfidTag(scenario.rfidTagId());
        CustomerId waitingCustomer = this.fixture.given().customerWaitingForRfidTag();
        CustomerId otherCustomer = this.fixture.given().customer();

        // when
        MvcTestResult result = this.fixture.api().requestRfidTagAssignment(otherCustomer, RfidTagAssignmentType.FIXED);

        // then
        this.fixture.then().expectProblem(result, HttpStatus.CONFLICT, RfidTagAssignmentAlreadyWaitingException.CODE);
        this.fixture.then().expectNoRfidTagAssignment(otherCustomer);

        // when
        this.fixture.then().expectSuccess(this.fixture.api().assignRfidTag(rfidTagId));
        result = this.fixture.api().requestRfidTagAssignment(otherCustomer, RfidTagAssignmentType.FIXED);

        // then
        this.fixture.then().expectSuccess(result);
        this.fixture.then().expectRfidTagAssigned(waitingCustomer, rfidTagId);
        this.fixture.then().expectWaitingForRfidTag(otherCustomer);

        // cleanup
        this.fixture.endWaitingRfidTagAssignment();
    }

    @Test
    void ta5_onlyAnAssignedTagCanBeUnassigned() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        RfidTagId rfidTagId = this.fixture.given().rfidTag(scenario.rfidTagId());
        CustomerId customerId = this.fixture.given().customerWaitingForRfidTag();

        // when
        MvcTestResult result = this.fixture.api().unassignRfidTag(customerId);

        // then
        this.fixture.then().expectProblem(result, HttpStatus.CONFLICT, NoRfidTagAssignedException.CODE);
        this.fixture.then().expectWaitingForRfidTag(customerId);

        // when
        this.fixture.given().rfidTagAssignedToTheWaitingCustomer(rfidTagId);
        result = this.fixture.api().unassignRfidTag(customerId);

        // then
        this.fixture.then().expectSuccess(result);
        this.fixture.then().expectNoRfidTagAssignment(customerId);
    }

    @Test
    void ta6_unassigningMovesTheAssignmentToTheHistory() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        RfidTagId rfidTagId = this.fixture.given().rfidTag(scenario.rfidTagId());
        CustomerId customerId = this.fixture.given().customerWithRfidTag(rfidTagId);

        // when
        MvcTestResult result = this.fixture.api().unassignRfidTag(customerId);

        // then
        this.fixture.then().expectSuccess(result);
        this.fixture.then().expectNoRfidTagAssignment(customerId);
        this.fixture.then().expectHistoryEntry(customerId, rfidTagId);
    }

    @Test
    void ta7_onlyAnExistingCustomerCanRequestATag() {
        // given
        CustomerId unknown = CustomerId.random();

        // when
        MvcTestResult result = this.fixture.api().requestRfidTagAssignment(unknown, RfidTagAssignmentType.FIXED);

        // then
        this.fixture.then().expectProblem(result, HttpStatus.NOT_FOUND, ResourceNotFoundException.CODE);
    }

    @Test
    void ta8_onlyARegisteredTagCanBeAssigned() {
        // given
        RfidTagId unregistered = RandomValues.rfidTagId();

        // when
        MvcTestResult result = this.fixture.api().assignRfidTag(unregistered);

        // then
        this.fixture.then().expectProblem(result, HttpStatus.NOT_FOUND, ResourceNotFoundException.CODE);
    }

}
