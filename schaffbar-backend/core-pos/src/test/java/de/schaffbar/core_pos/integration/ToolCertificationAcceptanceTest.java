package de.schaffbar.core_pos.integration;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import de.schaffbar.core_pos.integration.fixture.Scenario;
import de.schaffbar.core_pos.integration.fixture.SchaffbarFixture;
import de.schaffbar.core_pos.shared.exception.CertificationAlreadyExistsException;
import de.schaffbar.core_pos.shared.exception.CertificationPermanentlyRevokedException;
import de.schaffbar.core_pos.shared.exception.CustomerIsInstructorException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;

// one test per rule of domain/tool_certification/README.md that reads the database
@IntegrationTest
class ToolCertificationAcceptanceTest {

    @Autowired
    private SchaffbarFixture fixture;

    @Test
    void tc1_aCustomerHasOneCertificationPerTool() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        ToolId toolId = this.fixture.given().tool(scenario);
        CustomerId instructorId = this.fixture.given().instructor(toolId);
        CustomerId customerId = this.fixture.given().customer();
        this.fixture.given().toolCertification(customerId, toolId, instructorId);

        // when
        MvcTestResult result = this.fixture.api().certifyCustomer(customerId, toolId, instructorId);

        // then
        this.fixture.then().expectProblem(result, HttpStatus.CONFLICT, CertificationAlreadyExistsException.CODE);
        this.fixture.then().expectCertifiedCustomers(toolId, customerId);
    }

    @Test
    void tc2_aRevokedCertificationIsFinal() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        ToolId toolId = this.fixture.given().tool(scenario);
        CustomerId instructorId = this.fixture.given().instructor(toolId);
        CustomerId customerId = this.fixture.given().customer();
        this.fixture.given().toolCertification(customerId, toolId, instructorId);
        this.fixture.then().expectSuccess(this.fixture.api().revokeToolCertification(customerId, toolId));

        // when
        MvcTestResult result = this.fixture.api().certifyCustomer(customerId, toolId, instructorId);

        // then
        this.fixture.then().expectProblem(result, HttpStatus.CONFLICT, CertificationPermanentlyRevokedException.CODE);
        this.fixture.then().expectCertifiedCustomers(toolId, customerId);
    }

    @Test
    void tc4_aBatchCertifiesAllCustomersOrNobody() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        ToolId toolId = this.fixture.given().tool(scenario);
        CustomerId instructorId = this.fixture.given().instructor(toolId);
        CustomerId certified = this.fixture.given().customer();
        this.fixture.given().toolCertification(certified, toolId, instructorId);
        CustomerId customerId = this.fixture.given().customer();

        // when – the second customer is rejected after the first one was certified
        MvcTestResult result = this.fixture.api().certifyCustomers(List.of(customerId, certified), toolId, instructorId);

        // then
        this.fixture.then().expectProblem(result, HttpStatus.CONFLICT, CertificationAlreadyExistsException.CODE);
        this.fixture.then().expectCertifiedCustomers(toolId, certified);
    }

    @Test
    void tc5_anInstructorIsNotCertifiedForTheirTool() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        ToolId toolId = this.fixture.given().tool(scenario);
        CustomerId instructorId = this.fixture.given().instructor(toolId);
        CustomerId otherInstructorId = this.fixture.given().instructor(toolId);

        // when
        MvcTestResult result = this.fixture.api().certifyCustomer(otherInstructorId, toolId, instructorId);

        // then
        this.fixture.then().expectProblem(result, HttpStatus.CONFLICT, CustomerIsInstructorException.CODE);
        this.fixture.then().expectCertifiedCustomers(toolId);
    }

}
