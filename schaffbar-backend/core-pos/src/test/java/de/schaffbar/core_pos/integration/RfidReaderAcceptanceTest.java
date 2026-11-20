package de.schaffbar.core_pos.integration;

import static de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType.GATE_KEEPER_IN;
import static de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType.SWITCH_BOX;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import de.schaffbar.core_pos.integration.fixture.Scenario;
import de.schaffbar.core_pos.integration.fixture.SchaffbarFixture;
import de.schaffbar.core_pos.shared.exception.MacAddressAlreadyUsedException;
import de.schaffbar.core_pos.shared.exception.RfidReaderHasToolAssignedException;
import de.schaffbar.core_pos.shared.id.RfidReaderId;
import de.schaffbar.core_pos.shared.id.ToolId;

// one test per rule of domain/rfid_reader/README.md
@IntegrationTest
class RfidReaderAcceptanceTest {

    @Autowired
    private SchaffbarFixture fixture;

    @Test
    void r1_aMacAddressBelongsToOneReaderOnly() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        this.fixture.given().rfidReader(scenario.macAddress());

        // when – lower case is the same address (R-7)
        MvcTestResult result = this.fixture.api().createRfidReader(scenario.macAddress().getValue().toLowerCase());

        // then
        this.fixture.then().expectProblem(result, HttpStatus.CONFLICT, MacAddressAlreadyUsedException.CODE);
        this.fixture.then().expectRfidReaderCount(scenario.macAddress(), 1);
    }

    @Test
    void r3_theTypeOfASwitchBoxStaysWhileAToolIsAssigned() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        RfidReaderId switchBox = this.fixture.given().switchBox(scenario.macAddress());
        ToolId toolId = this.fixture.given().toolWithSwitchBox(scenario.toolName(), switchBox);

        // when
        MvcTestResult result = this.fixture.api().changeRfidReaderType(switchBox, GATE_KEEPER_IN);

        // then
        this.fixture.then().expectProblem(result, HttpStatus.CONFLICT, RfidReaderHasToolAssignedException.CODE);
        this.fixture.then().expectRfidReader(switchBox, SWITCH_BOX);

        // when
        MvcTestResult resultClear = this.fixture.api().clearRfidReader(toolId);
        this.fixture.then().expectSuccess(resultClear);

        result = this.fixture.api().changeRfidReaderType(switchBox, GATE_KEEPER_IN);

        // then
        this.fixture.then().expectSuccess(result);
        this.fixture.then().expectRfidReader(switchBox, GATE_KEEPER_IN);
    }

    @Test
    void r5_aReaderAssignedToAToolCannotBeDeleted() {
        Scenario scenario = Scenario.randomValues().build();

        // given
        RfidReaderId switchBox = this.fixture.given().switchBox(scenario.macAddress());
        ToolId toolId = this.fixture.given().toolWithSwitchBox(scenario.toolName(), switchBox);

        // when
        MvcTestResult result = this.fixture.api().deleteRfidReader(switchBox);

        // then
        this.fixture.then().expectProblem(result, HttpStatus.CONFLICT, RfidReaderHasToolAssignedException.CODE);
        this.fixture.then().expectRfidReader(switchBox, SWITCH_BOX);

        // when
        MvcTestResult resultClear = this.fixture.api().clearRfidReader(toolId);
        this.fixture.then().expectSuccess(resultClear);

        result = this.fixture.api().deleteRfidReader(switchBox);

        // then
        this.fixture.then().expectSuccess(result);
        this.fixture.then().expectNoRfidReader(switchBox);
    }

}
