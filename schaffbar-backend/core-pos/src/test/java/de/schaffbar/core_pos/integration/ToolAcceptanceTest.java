package de.schaffbar.core_pos.integration;

import static de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType.GATE_KEEPER_IN;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import de.schaffbar.core_pos.builder.RandomValues;
import de.schaffbar.core_pos.integration.fixture.Scenario;
import de.schaffbar.core_pos.integration.fixture.SchaffbarFixture;
import de.schaffbar.core_pos.shared.exception.InvalidRfidReaderTypeForToolException;
import de.schaffbar.core_pos.shared.exception.IpAddressAlreadyUsedException;
import de.schaffbar.core_pos.shared.exception.RfidReaderAlreadyAssignedToToolException;
import de.schaffbar.core_pos.shared.exception.ToolInUseException;
import de.schaffbar.core_pos.shared.exception.ToolNameAlreadyUsedException;
import de.schaffbar.core_pos.shared.id.MacAddress;
import de.schaffbar.core_pos.shared.id.RfidReaderId;
import de.schaffbar.core_pos.shared.id.RfidTagId;
import de.schaffbar.core_pos.shared.id.ToolId;

// one test per rule of domain/tool/README.md that reads the database
@IntegrationTest
class ToolAcceptanceTest {

    @Autowired
    private SchaffbarFixture fixture;

    @Test
    void t2_t15_toolNamesAreUniqueWithoutRegardToCase() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        ToolId toolId = this.fixture.given().tool(scenario);
        ToolId otherTool = this.fixture.given().tool(RandomValues.toolName());

        // when
        MvcTestResult created = this.fixture.api().createTool(scenario.toolName().toUpperCase());
        MvcTestResult renamed = this.fixture.api().updateTool(otherTool, scenario.toolName().toLowerCase());

        // then
        this.fixture.then().expectProblem(created, HttpStatus.CONFLICT, ToolNameAlreadyUsedException.CODE);
        this.fixture.then().expectProblem(renamed, HttpStatus.CONFLICT, ToolNameAlreadyUsedException.CODE);

        // when – the tool itself may change the case of its name
        MvcTestResult result = this.fixture.api().updateTool(toolId, scenario.toolName().toUpperCase());

        // then
        this.fixture.then().expectSuccess(result);
        this.fixture.then().expectTool(toolId, scenario.toolName().toUpperCase());
    }

    @Test
    void t3_t8_aSwitchBoxBelongsToOneToolOnly() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        RfidReaderId switchBox = this.fixture.given().switchBox(scenario.macAddress());
        ToolId toolId = this.fixture.given().toolWithSwitchBox(scenario, switchBox);
        ToolId otherTool = this.fixture.given().tool(RandomValues.toolName());

        // when
        MvcTestResult result = this.fixture.api().assignRfidReader(otherTool, switchBox);

        // then
        this.fixture.then().expectProblem(result, HttpStatus.CONFLICT, RfidReaderAlreadyAssignedToToolException.CODE);
        this.fixture.then().expectNoSwitchBox(otherTool);

        // when – the same tool again changes nothing (T-8)
        result = this.fixture.api().assignRfidReader(toolId, switchBox);

        // then
        this.fixture.then().expectSuccess(result);
        this.fixture.then().expectSwitchBox(toolId, switchBox);
    }

    @Test
    void t4_anIpAddressBelongsToOneToolOnly() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        ToolId toolId = this.fixture.given().toolWithWlanRelais(scenario);
        ToolId otherTool = this.fixture.given().tool(RandomValues.toolName());

        // when
        MvcTestResult result = this.fixture.api().setWlanRelais(otherTool, scenario.ipAddress());

        // then
        this.fixture.then().expectProblem(result, HttpStatus.CONFLICT, IpAddressAlreadyUsedException.CODE);

        // when – the first tool gives the address up
        this.fixture.then().expectSuccess(this.fixture.api().clearWlanRelais(toolId));
        result = this.fixture.api().setWlanRelais(otherTool, scenario.ipAddress());

        // then
        this.fixture.then().expectSuccess(result);
        this.fixture.then().expectWlanRelais(otherTool, scenario.ipAddress());
    }

    @Test
    void t9_onlyASwitchBoxCanBeAssigned() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        RfidReaderId gateKeeper = this.fixture.given().rfidReader(scenario.macAddress(), GATE_KEEPER_IN);
        ToolId toolId = this.fixture.given().tool(scenario);

        // when
        MvcTestResult result = this.fixture.api().assignRfidReader(toolId, gateKeeper);

        // then
        this.fixture.then().expectProblem(result, HttpStatus.CONFLICT, InvalidRfidReaderTypeForToolException.CODE);
        this.fixture.then().expectNoSwitchBox(toolId);
    }

    @Test
    void t14_t17_aToolInUseKeepsItsSwitchBoxAndCannotBeDeleted() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        MacAddress gateKeeperIn = RandomValues.macAddress();
        this.fixture.given().rfidReader(gateKeeperIn, GATE_KEEPER_IN);
        RfidReaderId switchBox = this.fixture.given().switchBox(scenario.macAddress());
        ToolId toolId = this.fixture.given().toolWithSwitchBox(scenario, switchBox);
        RfidTagId rfidTagId = this.fixture.given().rfidTag(scenario.rfidTagId());
        this.fixture.given().customerInTheWorkshop(rfidTagId, gateKeeperIn);
        this.fixture.given().toolUsage(rfidTagId, scenario.macAddress());

        // when
        MvcTestResult cleared = this.fixture.api().clearRfidReader(toolId);
        MvcTestResult deleted = this.fixture.api().deleteTool(toolId);

        // then
        this.fixture.then().expectProblem(cleared, HttpStatus.CONFLICT, ToolInUseException.CODE);
        this.fixture.then().expectProblem(deleted, HttpStatus.CONFLICT, ToolInUseException.CODE);
        this.fixture.then().expectSwitchBox(toolId, switchBox);

        // when – the person ends the usage at the switch box
        this.fixture.then().expectSuccess(this.fixture.api().holdRfidTagToReader(rfidTagId, scenario.macAddress()));
        cleared = this.fixture.api().clearRfidReader(toolId);
        deleted = this.fixture.api().deleteTool(toolId);

        // then
        this.fixture.then().expectSuccess(cleared);
        this.fixture.then().expectSuccess(deleted);
        this.fixture.then().expectNoTool(toolId);
    }

}
