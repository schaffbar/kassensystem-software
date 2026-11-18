package de.schaffbar.core_pos.integration.fixture;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.IpAddress;
import de.schaffbar.core_pos.shared.id.MacAddress;
import de.schaffbar.core_pos.shared.id.RfidReaderId;
import de.schaffbar.core_pos.shared.id.RfidTagId;
import de.schaffbar.core_pos.shared.id.ToolId;

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
    // customers

    public void expectCustomer(CustomerId customerId) {
        assertThat(this.api.getCustomer(customerId)).hasStatus(HttpStatus.OK);
    }

    public void expectNoCustomer(CustomerId customerId) {
        expectProblem(this.api.getCustomer(customerId), HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    // ------------------------------------------------------------------------
    // RFID readers

    public void expectRfidReader(RfidReaderId rfidReaderId, RfidReaderType type) {
        MvcTestResult result = this.api.getRfidReader(rfidReaderId);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.type").isEqualTo(type.name());
    }

    public void expectNoRfidReader(RfidReaderId rfidReaderId) {
        expectProblem(this.api.getRfidReader(rfidReaderId), HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
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
        expectProblem(this.api.getRfidTag(rfidTagId), HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
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
        expectProblem(this.api.getTool(toolId), HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
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

    // ------------------------------------------------------------------------
    // helper

    private Object[] values(CustomerId... customerIds) {
        return Arrays.stream(customerIds) //
                .map(customerId -> customerId.getValue().toString()) //
                .toArray();
    }

}
