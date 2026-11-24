package de.schaffbar.core_pos.integration.fixture;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;

import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.IpAddress;
import de.schaffbar.core_pos.shared.id.MacAddress;
import de.schaffbar.core_pos.shared.id.RfidReaderId;
import de.schaffbar.core_pos.shared.id.RfidTagId;
import de.schaffbar.core_pos.shared.id.ToolId;
import de.schaffbar.core_pos.support.Json;

public final class ThenFixture {

    private final SchaffbarApi api;

    ThenFixture(SchaffbarApi api) {
        this.api = api;
    }

    // ------------------------------------------------------------------------
    // responses

    public void expectSuccess(MvcTestResult result) {
        assertThat(result).hasStatus2xxSuccessful();
    }

    public void expectProblem(MvcTestResult result, HttpStatus status, String code) {
        assertThat(result).hasStatus(status);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(code);
    }

    public void expectBadRequest(MvcTestResult result) {
        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    // ------------------------------------------------------------------------
    // devices

    // a device always gets 200; every field written in the expectation must match, fields left out are not checked
    public void expectDeviceAnswer(MvcTestResult result, String expectedJson) {
        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().isLenientlyEqualTo(expectedJson);
    }

    // the times change with every run – the gate keeper shows them as HH:mm, the units as a number
    public void expectEntryTimeShown(MvcTestResult result) {
        assertThat(Json.read(result, "$.CUSTOMERSTARTSTOP")).matches("\\d{2}:\\d{2}");
        assertThat(Json.read(result, "$.UNITS")).matches("\\d+");
    }

    public void expectEntryAndExitTimeShown(MvcTestResult result) {
        assertThat(Json.read(result, "$.CUSTOMERSTARTSTOP")).matches("\\d{2}:\\d{2} - \\d{2}:\\d{2}");
        assertThat(Json.read(result, "$.UNITS")).matches("\\d+");
    }

    // ------------------------------------------------------------------------
    // customers

    public void expectCustomer(CustomerId customerId) {
        assertThat(this.api.getCustomer(customerId)).hasStatus(HttpStatus.OK);
    }

    // the date of birth as the portal reads it (z.iso.date(): "1990-01-31")
    public void expectCreatedCustomerBornOn(MvcTestResult result, LocalDate dateOfBirth) {
        assertThat(result).hasStatus(HttpStatus.CREATED);
        MvcTestResult customer = this.api.getCustomer(CustomerId.of(GivenFixture.createdId(result)));

        assertThat(customer).bodyJson().extractingPath("$.dateOfBirth").isEqualTo(dateOfBirth.toString());
    }

    public void expectNoCustomer(CustomerId customerId) {
        expectProblem(this.api.getCustomer(customerId), HttpStatus.NOT_FOUND, ResourceNotFoundException.CODE);
    }

    // ------------------------------------------------------------------------
    // RFID readers

    public void expectRfidReader(RfidReaderId rfidReaderId, RfidReaderType type) {
        MvcTestResult result = this.api.getRfidReader(rfidReaderId);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.type").isEqualTo(type.name());
    }

    public void expectNoRfidReader(RfidReaderId rfidReaderId) {
        expectProblem(this.api.getRfidReader(rfidReaderId), HttpStatus.NOT_FOUND, ResourceNotFoundException.CODE);
    }

    public void expectRfidReaderCount(MacAddress macAddress, int count) {
        MvcTestResult result = this.api.getRfidReaders(macAddress);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$").asArray().hasSize(count);
    }

    // ------------------------------------------------------------------------
    // RFID tags

    public void expectRfidTag(RfidTagId rfidTagId) {
        assertThat(this.api.getRfidTag(rfidTagId)).hasStatus(HttpStatus.OK);
    }

    public void expectNoRfidTag(RfidTagId rfidTagId) {
        expectProblem(this.api.getRfidTag(rfidTagId), HttpStatus.NOT_FOUND, ResourceNotFoundException.CODE);
    }

    // ------------------------------------------------------------------------
    // RFID tag assignments

    public void expectRfidTagAssigned(CustomerId customerId, RfidTagId rfidTagId) {
        MvcTestResult result = this.api.getRfidTagAssignments(customerId);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$[0].status").isEqualTo("ASSIGNED");
        assertThat(result).bodyJson().extractingPath("$[0].rfidTagId").isEqualTo(rfidTagId.getValue());
    }

    public void expectWaitingForRfidTag(CustomerId customerId) {
        MvcTestResult result = this.api.getRfidTagAssignments(customerId);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$[0].status").isEqualTo("WAITING_FOR_ASSIGNMENT");
    }

    public void expectNoRfidTagAssignment(CustomerId customerId) {
        MvcTestResult result = this.api.getRfidTagAssignments(customerId);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$").asArray().isEmpty();
    }

    public void expectHistoryEntry(CustomerId customerId, RfidTagId rfidTagId) {
        MvcTestResult result = this.api.getRfidTagAssignmentHistory();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson() //
                .extractingPath("$[?(@.customerId == '%s')].rfidTagId".formatted(customerId.getValue())) //
                .asArray() //
                .containsExactly(rfidTagId.getValue());
    }

    // ------------------------------------------------------------------------
    // tools

    public void expectTool(ToolId toolId, String name) {
        MvcTestResult result = this.api.getTool(toolId);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.name").isEqualTo(name);
    }

    public void expectNoTool(ToolId toolId) {
        expectProblem(this.api.getTool(toolId), HttpStatus.NOT_FOUND, ResourceNotFoundException.CODE);
    }

    public void expectSwitchBox(ToolId toolId, RfidReaderId rfidReaderId) {
        MvcTestResult result = this.api.getTool(toolId);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.rfidReaderId").isEqualTo(rfidReaderId.getValue().toString());
    }

    public void expectNoSwitchBox(ToolId toolId) {
        MvcTestResult result = this.api.getTool(toolId);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.rfidReaderId").isNull();
    }

    public void expectWlanRelais(ToolId toolId, IpAddress ipAddress) {
        MvcTestResult result = this.api.getTool(toolId);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.ipAddress").isEqualTo(ipAddress.getValue());
    }

    public void expectInstructors(ToolId toolId, CustomerId... instructorIds) {
        MvcTestResult result = this.api.getTool(toolId);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson() //
                .extractingPath("$.instructors") //
                .asArray() //
                .containsExactlyInAnyOrder(values(instructorIds));
    }

    // ------------------------------------------------------------------------
    // tool certifications

    public void expectCertifiedCustomers(ToolId toolId, CustomerId... customerIds) {
        MvcTestResult result = this.api.getToolCertifications(toolId);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson() //
                .extractingPath("$[*].customerId") //
                .asArray() //
                .containsExactlyInAnyOrder(values(customerIds));
    }

    public void expectCertifiedBy(ToolId toolId, CustomerId customerId, CustomerId certifiedBy) {
        MvcTestResult result = this.api.getToolCertifications(toolId);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson() //
                .extractingPath("$[?(@.customerId == '%s')].certifiedBy".formatted(customerId.getValue())) //
                .asArray() //
                .containsExactly(certifiedBy.getValue().toString());
    }

    // ------------------------------------------------------------------------
    // workshop sessions

    // returns the id of the session, to tell it from a later one
    public String expectOpenWorkshopSession(CustomerId customerId) {
        MvcTestResult result = this.api.getOpenWorkshopSession(customerId);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("OPEN");
        return Json.read(result, "$.id");
    }

    // an open session other than the one before – the next visit after billing starts a new one
    public String expectNewWorkshopSession(CustomerId customerId, String previousSessionId) {
        String sessionId = expectOpenWorkshopSession(customerId);
        assertThat(sessionId).isNotEqualTo(previousSessionId);

        return sessionId;
    }

    // without an open session the body is empty
    public void expectNoOpenWorkshopSession(CustomerId customerId) {
        MvcTestResult result = this.api.getOpenWorkshopSession(customerId);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).hasBodyTextEqualTo("");
    }

    // ------------------------------------------------------------------------
    // workshop usages

    // the visits of the open session, checked out or not
    public void expectVisits(CustomerId customerId, int count) {
        assertThat(this.api.getOpenWorkshopSession(customerId)).bodyJson() //
                .extractingPath("$.workshopUsages") //
                .asArray() //
                .hasSize(count);
    }

    public void expectInTheWorkshop(CustomerId customerId) {
        assertThat(this.api.getOpenWorkshopSession(customerId)).bodyJson() //
                .extractingPath("$.workshopUsages[?(@.exitTime == null)]") //
                .asArray() //
                .hasSize(1);
    }

    public void expectCheckedOut(CustomerId customerId) {
        assertThat(this.api.getOpenWorkshopSession(customerId)).bodyJson() //
                .extractingPath("$.workshopUsages[?(@.exitTime == null)]") //
                .asArray() //
                .isEmpty();
    }

    // ------------------------------------------------------------------------
    // tool usages

    public void expectToolRunning(CustomerId customerId, ToolId toolId) {
        assertThat(this.api.getOpenWorkshopSession(customerId)).bodyJson() //
                .extractingPath("$.toolUsageSummaries[?(@.toolId == '%s')].running".formatted(toolId.getValue())) //
                .asArray() //
                .containsExactly(true);
    }

    // the customer has no running usage of the tool – also when they have no open session at all
    public void expectToolNotRunningFor(CustomerId customerId, ToolId toolId) {
        MvcTestResult result = this.api.getOpenWorkshopSession(customerId);
        assertThat(result).hasStatus(HttpStatus.OK);
        if (result.getResponse().getContentAsByteArray().length == 0) {
            return;
        }

        assertThat(result).bodyJson() //
                .extractingPath("$.toolUsageSummaries[?(@.toolId == '%s' && @.running == true)]".formatted(toolId.getValue())) //
                .asArray() //
                .isEmpty();
    }

    public void expectToolStopped(CustomerId customerId, ToolId toolId) {
        assertThat(this.api.getOpenWorkshopSession(customerId)).bodyJson() //
                .extractingPath("$.toolUsageSummaries[?(@.toolId == '%s')].running".formatted(toolId.getValue())) //
                .asArray() //
                .containsExactly(false);
    }

    // ------------------------------------------------------------------------
    // helper

    private Object[] values(CustomerId... customerIds) {
        return Arrays.stream(customerIds) //
                .map(customerId -> customerId.getValue().toString()) //
                .toArray();
    }

}
