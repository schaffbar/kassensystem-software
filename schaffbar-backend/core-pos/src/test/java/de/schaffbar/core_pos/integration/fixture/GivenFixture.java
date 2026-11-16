package de.schaffbar.core_pos.integration.fixture;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentType;
import de.schaffbar.core_pos.shared.id.CustomerId;
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

    public RfidReaderId rfidReader(Scenario scenario) {
        MvcTestResult result = this.api.createRfidReader(scenario.macAddress());
        assertThat(result).hasStatus(HttpStatus.CREATED);

        return RfidReaderId.of(SchaffbarApi.createdId(result));
    }

    public RfidReaderId switchBox(Scenario scenario) {
        RfidReaderId rfidReaderId = rfidReader(scenario);

        MvcTestResult result = this.api.changeRfidReaderType(rfidReaderId, RfidReaderType.SWITCH_BOX);
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);

        return rfidReaderId;
    }

    // ------------------------------------------------------------------------
    // RFID tags

    public RfidTagId rfidTag(Scenario scenario) {
        MvcTestResult result = this.api.createRfidTag(scenario.rfidTagId());
        assertThat(result).hasStatus(HttpStatus.CREATED);

        return scenario.rfidTagId();
    }

    // ------------------------------------------------------------------------
    // customers

    public CustomerId customer() {
        MvcTestResult result = this.api.createCustomer();
        assertThat(result).hasStatus(HttpStatus.CREATED);

        return CustomerId.of(SchaffbarApi.createdId(result));
    }

    public CustomerId customerWithRfidTag(RfidTagId rfidTagId) {
        CustomerId customerId = customer();

        MvcTestResult requested = this.api.requestRfidTagAssignment(customerId, RfidTagAssignmentType.FIXED);
        assertThat(requested).hasStatus(HttpStatus.OK);

        MvcTestResult assigned = this.api.assignRfidTag(rfidTagId);
        assertThat(assigned).hasStatus(HttpStatus.OK);

        return customerId;
    }

    // ------------------------------------------------------------------------
    // tools

    public ToolId tool(Scenario scenario) {
        MvcTestResult result = this.api.createTool(scenario.toolName());
        assertThat(result).hasStatus(HttpStatus.CREATED);

        return ToolId.of(SchaffbarApi.createdId(result));
    }

    public ToolId toolWithSwitchBox(Scenario scenario, RfidReaderId switchBox) {
        ToolId toolId = tool(scenario);

        MvcTestResult result = this.api.assignRfidReader(toolId, switchBox);
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);

        return toolId;
    }

}
