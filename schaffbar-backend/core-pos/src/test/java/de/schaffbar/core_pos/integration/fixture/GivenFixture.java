package de.schaffbar.core_pos.integration.fixture;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentType;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.MacAddress;
import de.schaffbar.core_pos.shared.id.RfidReaderId;
import de.schaffbar.core_pos.shared.id.RfidTagId;
import de.schaffbar.core_pos.shared.id.ToolId;

public final class GivenFixture {

    private final SchaffbarApi api;

    GivenFixture(SchaffbarApi api) {
        this.api = api;
    }

    // ------------------------------------------------------------------------
    // RFID readers

    public RfidReaderId rfidReader(MacAddress macAddress) {
        MvcTestResult result = this.api.createRfidReader(macAddress);
        assertThat(result).hasStatus(HttpStatus.CREATED);

        return RfidReaderId.of(createdId(result));
    }

    public RfidReaderId rfidReader(MacAddress macAddress, RfidReaderType type) {
        RfidReaderId rfidReaderId = rfidReader(macAddress);

        MvcTestResult result = this.api.changeRfidReaderType(rfidReaderId, type);
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);

        return rfidReaderId;
    }

    public RfidReaderId switchBox(MacAddress macAddress) {
        return rfidReader(macAddress, RfidReaderType.SWITCH_BOX);
    }

    // ------------------------------------------------------------------------
    // RFID tags

    public RfidTagId rfidTag(RfidTagId id) {
        MvcTestResult result = this.api.createRfidTag(id);
        assertThat(result).hasStatus(HttpStatus.CREATED);

        return id;
    }

    // ------------------------------------------------------------------------
    // customers

    public CustomerId customer() {
        MvcTestResult result = this.api.createCustomer();
        assertThat(result).hasStatus(HttpStatus.CREATED);

        return CustomerId.of(createdId(result));
    }

    public CustomerId customerWaitingForRfidTag() {
        CustomerId customerId = customer();

        MvcTestResult requested = this.api.requestRfidTagAssignment(customerId, RfidTagAssignmentType.FIXED);
        assertThat(requested).hasStatus(HttpStatus.NO_CONTENT);

        return customerId;
    }

    public CustomerId customerWithRfidTag(RfidTagId rfidTagId) {
        CustomerId customerId = customerWaitingForRfidTag();
        rfidTagAssignedToTheWaitingCustomer(rfidTagId);

        return customerId;
    }

    public void rfidTagAssignedToTheWaitingCustomer(RfidTagId rfidTagId) {
        MvcTestResult assigned = this.api.assignRfidTag(rfidTagId);
        assertThat(assigned).hasStatus(HttpStatus.NO_CONTENT);
    }

    // checked in at a reader of type GATE_KEEPER_IN – the first check-in opens the session
    public CustomerId customerInTheWorkshop(RfidTagId rfidTagId, MacAddress gateKeeperIn) {
        CustomerId customerId = customerWithRfidTag(rfidTagId);

        MvcTestResult checkedIn = this.api.holdRfidTagToReader(rfidTagId, gateKeeperIn);
        assertThat(checkedIn).hasStatus(HttpStatus.OK);

        MvcTestResult session = this.api.getOpenWorkshopSession(customerId);
        assertThat(session).bodyJson().extractingPath("$.status").isEqualTo("OPEN");

        return customerId;
    }

    // ------------------------------------------------------------------------
    // tools

    public ToolId tool(Scenario scenario) {
        MvcTestResult result = this.api.createTool(scenario.toolName());
        assertThat(result).hasStatus(HttpStatus.CREATED);

        return ToolId.of(createdId(result));
    }

    public CustomerId instructor(ToolId toolId) {
        CustomerId instructorId = customer();

        MvcTestResult result = this.api.addInstructor(toolId, instructorId);
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);

        return instructorId;
    }

    public void toolCertification(CustomerId customerId, ToolId toolId, CustomerId certifiedBy) {
        MvcTestResult result = this.api.certifyCustomer(customerId, toolId, certifiedBy);
        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.failed").asArray().isEmpty();
    }

    public ToolId toolWithSwitchBox(Scenario scenario, RfidReaderId switchBox) {
        ToolId toolId = tool(scenario);

        MvcTestResult result = this.api.assignRfidReader(toolId, switchBox);
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);

        return toolId;
    }

    // ------------------------------------------------------------------------
    // helper

    private UUID createdId(MvcTestResult result) {
        String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }

}
