package de.schaffbar.core_pos.integration;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import de.schaffbar.core_pos.builder.RandomValues;
import de.schaffbar.core_pos.integration.fixture.Scenario;
import de.schaffbar.core_pos.integration.fixture.SchaffbarFixture;
import de.schaffbar.core_pos.shared.exception.CertificationAlreadyExistsException;
import de.schaffbar.core_pos.shared.exception.CertificationPermanentlyRevokedException;
import de.schaffbar.core_pos.shared.exception.CustomerIsInstructorException;
import de.schaffbar.core_pos.shared.exception.CustomerNotInstructorException;
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
        ToolId toolId = this.fixture.given().tool(scenario.toolName());
        CustomerId instructorId = this.fixture.given().instructor(toolId);
        CustomerId customerId = this.fixture.given().customer();
        this.fixture.given().toolCertification(customerId, toolId, instructorId);

        // when
        MvcTestResult result = this.fixture.api().certifyCustomer(customerId, toolId, instructorId);

        // then
        this.fixture.then().expectProblem(result, HttpStatus.CONFLICT, CertificationAlreadyExistsException.CODE);
        this.fixture.then().expectCertifiedCustomers(toolId, customerId);

        // when – another tool
        ToolId otherTool = this.fixture.given().tool(RandomValues.toolName());
        CustomerId otherInstructorId = this.fixture.given().instructor(otherTool);
        result = this.fixture.api().certifyCustomer(customerId, otherTool, otherInstructorId);

        // then
        this.fixture.then().expectSuccess(result);
        this.fixture.then().expectCertifiedCustomers(otherTool, customerId);
    }

    // the changes find the certification by person and tool – the key of TC-1
    @Test
    void tc1_aChangeFindsTheCertificationOfThePersonForThatTool() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        ToolId toolId = this.fixture.given().tool(scenario.toolName());
        CustomerId instructorId = this.fixture.given().instructor(toolId);
        CustomerId customerId = this.fixture.given().customer();
        this.fixture.given().toolCertification(customerId, toolId, instructorId);
        ToolId otherTool = this.fixture.given().tool(RandomValues.toolName());

        // when
        MvcTestResult paused = this.fixture.api().pauseToolCertification(customerId, otherTool);
        MvcTestResult reactivated = this.fixture.api().reactivateToolCertification(customerId, otherTool);
        MvcTestResult revoked = this.fixture.api().revokeToolCertification(customerId, otherTool);
        MvcTestResult deleted = this.fixture.api().deleteToolCertification(customerId, otherTool);

        // then
        this.fixture.then().expectProblem(paused, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
        this.fixture.then().expectProblem(reactivated, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
        this.fixture.then().expectProblem(revoked, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
        this.fixture.then().expectProblem(deleted, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
        this.fixture.then().expectCertifiedCustomers(toolId, customerId);
    }

    // a revoked certification has no way back – there is no allowed side
    @Test
    void tc2_aRevokedCertificationIsFinal() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        ToolId toolId = this.fixture.given().tool(scenario.toolName());
        CustomerId instructorId = this.fixture.given().instructor(toolId);
        CustomerId customerId = this.fixture.given().customer();
        this.fixture.given().revokedToolCertification(customerId, toolId, instructorId);

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
        ToolId toolId = this.fixture.given().tool(scenario.toolName());
        CustomerId instructorId = this.fixture.given().instructor(toolId);
        CustomerId certified = this.fixture.given().customer();
        this.fixture.given().toolCertification(certified, toolId, instructorId);
        CustomerId customerId = this.fixture.given().customer();

        // when – the second customer is rejected after the first one was certified
        MvcTestResult result = this.fixture.api().certifyCustomers(List.of(customerId, certified), toolId, instructorId);

        // then
        this.fixture.then().expectProblem(result, HttpStatus.CONFLICT, CertificationAlreadyExistsException.CODE);
        this.fixture.then().expectCertifiedCustomers(toolId, certified);

        // when – the existing certification is gone
        this.fixture.then().expectSuccess(this.fixture.api().deleteToolCertification(certified, toolId));
        result = this.fixture.api().certifyCustomers(List.of(customerId, certified), toolId, instructorId);

        // then
        this.fixture.then().expectSuccess(result);
        this.fixture.then().expectCertifiedCustomers(toolId, customerId, certified);
    }

    @Test
    void tc5_anInstructorIsNotCertifiedForTheirTool() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        ToolId toolId = this.fixture.given().tool(scenario.toolName());
        CustomerId instructorId = this.fixture.given().instructor(toolId);
        CustomerId otherInstructorId = this.fixture.given().instructor(toolId);

        // when
        MvcTestResult result = this.fixture.api().certifyCustomer(otherInstructorId, toolId, instructorId);

        // then
        this.fixture.then().expectProblem(result, HttpStatus.CONFLICT, CustomerIsInstructorException.CODE);
        this.fixture.then().expectCertifiedCustomers(toolId);

        // when – no instructor any more
        this.fixture.then().expectSuccess(this.fixture.api().removeInstructor(toolId, otherInstructorId));
        result = this.fixture.api().certifyCustomer(otherInstructorId, toolId, instructorId);

        // then
        this.fixture.then().expectSuccess(result);
        this.fixture.then().expectCertifiedCustomers(toolId, otherInstructorId);
    }

    @Test
    void tc8_onlyAnInstructorOfTheToolCertifiesForIt() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        ToolId toolId = this.fixture.given().tool(scenario.toolName());
        CustomerId certifier = this.fixture.given().customer();
        CustomerId customerId = this.fixture.given().customer();

        // when
        MvcTestResult result = this.fixture.api().certifyCustomer(customerId, toolId, certifier);

        // then
        this.fixture.then().expectProblem(result, HttpStatus.CONFLICT, CustomerNotInstructorException.CODE);
        this.fixture.then().expectCertifiedCustomers(toolId);

        // when – the certifier becomes an instructor of the tool
        this.fixture.then().expectSuccess(this.fixture.api().addInstructor(toolId, certifier));
        result = this.fixture.api().certifyCustomer(customerId, toolId, certifier);

        // then
        this.fixture.then().expectSuccess(result);
        this.fixture.then().expectCertifiedCustomers(toolId, customerId);
    }

}
