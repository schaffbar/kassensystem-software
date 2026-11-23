package de.schaffbar.core_pos.integration;

import static de.schaffbar.core_pos.builder.CommandBuilder.createCustomerCommand;
import static de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType.GATE_KEEPER_IN;
import static de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType.GATE_KEEPER_OUT;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import de.schaffbar.core_pos.builder.RandomValues;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentType;
import de.schaffbar.core_pos.integration.fixture.Scenario;
import de.schaffbar.core_pos.integration.fixture.SchaffbarFixture;
import de.schaffbar.core_pos.shared.exception.CustomerHasOpenWorkshopSessionException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.MacAddress;
import de.schaffbar.core_pos.shared.id.RfidTagId;
import de.schaffbar.core_pos.shared.id.ToolId;
import de.schaffbar.core_pos.shared.time.WorkshopTime;

// one test per rule of domain/customer/README.md; each ends without a waiting RFID tag request (TA-3)
@IntegrationTest
class CustomerAcceptanceTest {

    @Autowired
    private SchaffbarFixture fixture;

    @Test
    void c4_deletingACustomerFreesTheirRfidTag() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        RfidTagId rfidTagId = this.fixture.given().rfidTag(scenario.rfidTagId());
        CustomerId customerId = this.fixture.given().customerWithRfidTag(rfidTagId);

        // when
        MvcTestResult result = this.fixture.api().deleteCustomer(customerId);

        // then
        this.fixture.then().expectSuccess(result);
        this.fixture.then().expectNoCustomer(customerId);
        this.fixture.then().expectHistoryEntry(customerId, rfidTagId);

        // when – the tag is free for the next customer
        CustomerId otherCustomer = this.fixture.given().customer();
        this.fixture.then().expectSuccess(this.fixture.api().requestRfidTagAssignment(otherCustomer, RfidTagAssignmentType.FIXED));
        result = this.fixture.api().assignRfidTag(rfidTagId);

        // then
        this.fixture.then().expectSuccess(result);
        this.fixture.then().expectRfidTagAssigned(otherCustomer, rfidTagId);
    }

    @Test
    void c4_deletingACustomerCancelsTheirWaitingRequest() {
        // given
        CustomerId waitingCustomer = this.fixture.given().customerWaitingForRfidTag();

        // when
        MvcTestResult result = this.fixture.api().deleteCustomer(waitingCustomer);

        // then
        this.fixture.then().expectSuccess(result);
        this.fixture.then().expectNoCustomer(waitingCustomer);

        // when – no request waits any more (TA-3)
        CustomerId otherCustomer = this.fixture.given().customer();
        result = this.fixture.api().requestRfidTagAssignment(otherCustomer, RfidTagAssignmentType.FIXED);

        // then
        this.fixture.then().expectSuccess(result);
        this.fixture.then().expectWaitingForRfidTag(otherCustomer);

        // cleanup
        this.fixture.endWaitingRfidTagAssignment();
    }

    @Test
    void c5_aCustomerWithAnOpenSessionCannotBeDeleted() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        MacAddress gateKeeperIn = scenario.macAddress();
        MacAddress gateKeeperOut = RandomValues.macAddress();
        this.fixture.given().rfidReader(gateKeeperIn, GATE_KEEPER_IN);
        this.fixture.given().rfidReader(gateKeeperOut, GATE_KEEPER_OUT);
        RfidTagId rfidTagId = this.fixture.given().rfidTag(scenario.rfidTagId());
        CustomerId customerId = this.fixture.given().customerInTheWorkshop(rfidTagId, gateKeeperIn);

        // when
        MvcTestResult result = this.fixture.api().deleteCustomer(customerId);

        // then
        this.fixture.then().expectProblem(result, HttpStatus.CONFLICT, CustomerHasOpenWorkshopSessionException.CODE);
        this.fixture.then().expectCustomer(customerId);

        // when – checked out and billed
        this.fixture.api().holdRfidTagToReader(rfidTagId, gateKeeperOut);
        this.fixture.then().expectCheckedOut(customerId);
        this.fixture.then().expectSuccess(this.fixture.api().closeWorkshopSession(customerId));

        result = this.fixture.api().deleteCustomer(customerId);

        // then
        this.fixture.then().expectSuccess(result);
        this.fixture.then().expectNoCustomer(customerId);
    }

    @Test
    void c6_theEmailIsAValidAddress() {
        // when
        MvcTestResult result = this.fixture.api().createCustomer(createCustomerCommand().email("erika.example.org").build());

        // then
        this.fixture.then().expectBadRequest(result);
    }

    @Test
    void c7_thePostalCodeHasFiveCharacters() {
        // when
        MvcTestResult result = this.fixture.api().createCustomer(createCustomerCommand().postalCode("1234").build());

        // then
        this.fixture.then().expectBadRequest(result);
    }

    @Test
    void c8_theDateOfBirthLiesWithinTheLast100YearsAndBeforeTheCurrentYear() {
        LocalDate thirtyYearsAgo = WorkshopTime.today().minusYears(30).withDayOfYear(31);

        // when
        MvcTestResult bornThisYear = this.fixture.api().createCustomer(createCustomerCommand().dateOfBirth(WorkshopTime.today().withDayOfYear(1)).build());
        MvcTestResult tooOld = this.fixture.api().createCustomer(createCustomerCommand().dateOfBirth(WorkshopTime.today().minusYears(101)).build());
        MvcTestResult bornThirtyYearsAgo = this.fixture.api().createCustomer(createCustomerCommand().dateOfBirth(thirtyYearsAgo).build());

        // then
        this.fixture.then().expectBadRequest(bornThisYear);
        this.fixture.then().expectBadRequest(tooOld);
        this.fixture.then().expectCreatedCustomerBornOn(bornThirtyYearsAgo, thirtyYearsAgo);
    }

    @Test
    void c9_c11_deletingACustomerDeletesTheirCertificationsAndInstructorRoleButKeepsTheCertificationsTheyGave() {
        Scenario scenario = Scenario.randomValues().build();

        // given – the customer is certified for one tool and instructs another (TC-5)
        ToolId certifiedTool = this.fixture.given().tool(scenario.toolName());
        CustomerId instructorOfTheCertifiedTool = this.fixture.given().instructor(certifiedTool);
        ToolId instructedTool = this.fixture.given().tool(RandomValues.toolName());
        CustomerId otherInstructor = this.fixture.given().instructor(instructedTool);
        CustomerId customerId = this.fixture.given().instructor(instructedTool);
        this.fixture.given().toolCertification(customerId, certifiedTool, instructorOfTheCertifiedTool);
        CustomerId certifiedByTheCustomer = this.fixture.given().customer();
        this.fixture.given().toolCertification(certifiedByTheCustomer, instructedTool, customerId);

        // when
        MvcTestResult result = this.fixture.api().deleteCustomer(customerId);

        // then
        this.fixture.then().expectSuccess(result);
        this.fixture.then().expectCertifiedCustomers(certifiedTool);
        this.fixture.then().expectInstructors(instructedTool, otherInstructor);
        this.fixture.then().expectCertifiedCustomers(instructedTool, certifiedByTheCustomer);
    }

}
