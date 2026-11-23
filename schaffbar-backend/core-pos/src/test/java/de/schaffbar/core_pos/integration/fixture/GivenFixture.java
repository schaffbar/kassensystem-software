package de.schaffbar.core_pos.integration.fixture;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import de.schaffbar.core_pos.builder.RandomValues;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentType;
import de.schaffbar.core_pos.domain.tool.CertificationRequirement;
import de.schaffbar.core_pos.domain.tool.WlanRelaisType;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.IpAddress;
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

    public CustomerId customerInTheWorkshop(RfidTagId rfidTagId, MacAddress gateKeeperIn) {
        CustomerId customerId = customerWithRfidTag(rfidTagId);

        MvcTestResult checkedIn = this.api.holdRfidTagToReader(rfidTagId, gateKeeperIn);
        assertThat(checkedIn).hasStatus(HttpStatus.OK);

        MvcTestResult session = this.api.getOpenWorkshopSession(customerId);
        assertThat(session).bodyJson().extractingPath("$.status").isEqualTo("OPEN");

        return customerId;
    }

    public CustomerId customerInTheWorkshop(RfidTagId rfidTagId) {
        MacAddress gateKeeperIn = RandomValues.macAddress();
        rfidReader(gateKeeperIn, RfidReaderType.GATE_KEEPER_IN);
        rfidTag(rfidTagId);

        return customerInTheWorkshop(rfidTagId, gateKeeperIn);
    }

    public void customerLeftTheWorkshop(RfidTagId rfidTagId, MacAddress gateKeeperOut) {
        MvcTestResult checkedOut = this.api.holdRfidTagToReader(rfidTagId, gateKeeperOut);
        assertThat(checkedOut).hasStatus2xxSuccessful();
    }

    // ------------------------------------------------------------------------
    // tools

    public ToolId tool(String name) {
        return tool(name, CertificationRequirement.YELLOW);
    }

    public ToolId tool(String name, CertificationRequirement certificationRequirement) {
        MvcTestResult result = this.api.createTool(name, certificationRequirement);
        assertThat(result).hasStatus(HttpStatus.CREATED);

        return ToolId.of(createdId(result));
    }

    public ToolId toolWithWlanRelais(String name, IpAddress ipAddress) {
        ToolId toolId = tool(name);

        MvcTestResult result = this.api.setWlanRelais(toolId, ipAddress);
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);

        return toolId;
    }

    public ToolId toolWithSwitchBox(String name, RfidReaderId switchBox) {
        return toolWithSwitchBox(name, switchBox, CertificationRequirement.YELLOW);
    }

    public ToolId toolWithSwitchBox(String name, RfidReaderId switchBox, CertificationRequirement certificationRequirement) {
        ToolId toolId = tool(name, certificationRequirement);

        MvcTestResult result = this.api.assignRfidReader(toolId, switchBox);
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);

        return toolId;
    }

    public ToolId toolWithSwitchBoxAndWlanRelais(String name, RfidReaderId switchBoxId, IpAddress ipAddress, WlanRelaisType type) {
        ToolId toolId = tool(name);

        MvcTestResult result = this.api.assignRfidReader(toolId, switchBoxId);
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);

        result = this.api.setWlanRelais(toolId, ipAddress, type);
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);

        return toolId;
    }

    public CustomerId instructor(ToolId toolId) {
        CustomerId instructorId = customer();

        MvcTestResult result = this.api.addInstructor(toolId, instructorId);
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);

        return instructorId;
    }

    public void toolCertification(CustomerId customerId, ToolId toolId, CustomerId certifiedBy) {
        MvcTestResult result = this.api.certifyCustomer(customerId, toolId, certifiedBy);
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
    }

    public void pausedToolCertification(CustomerId customerId, ToolId toolId, CustomerId certifiedBy) {
        toolCertification(customerId, toolId, certifiedBy);
        assertThat(this.api.pauseToolCertification(customerId, toolId)).hasStatus(HttpStatus.NO_CONTENT);
    }

    public void revokedToolCertification(CustomerId customerId, ToolId toolId, CustomerId certifiedBy) {
        toolCertification(customerId, toolId, certifiedBy);
        assertThat(this.api.revokeToolCertification(customerId, toolId)).hasStatus(HttpStatus.NO_CONTENT);
    }

    // the person is in the workshop and holds their tag to the switch box of the tool
    public void toolUsage(RfidTagId rfidTagId, MacAddress switchBox) {
        MvcTestResult result = this.api.holdRfidTagToReader(rfidTagId, switchBox);
        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.STATE").isEqualTo("WORKING");
    }

    // ------------------------------------------------------------------------
    // helper

    static UUID createdId(MvcTestResult result) {
        String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }

}
