package de.schaffbar.core_pos.integration.fixture;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import de.schaffbar.core_pos.builder.RandomValues;
import de.schaffbar.core_pos.shared.id.RfidTagId;

@TestComponent
public class SchaffbarFixture {

    private final SchaffbarApi api;

    private final GivenFixture given;

    private final ThenFixture then;

    SchaffbarFixture(MockMvcTester mvc) {
        this.api = new SchaffbarApi(mvc);
        this.given = new GivenFixture(this.api);
        this.then = new ThenFixture(this.api);
    }

    public GivenFixture given() {
        return this.given;
    }

    public SchaffbarApi api() {
        return this.api;
    }

    public ThenFixture then() {
        return this.then;
    }

    // ------------------------------------------------------------------------
    // cleanup

    // only one request may wait in the whole workshop (TA-3) – a test that leaves one ends with this, else the next
    // test that requests a tag fails
    public void endWaitingRfidTagAssignment() {
        RfidTagId rfidTagId = this.given.rfidTag(RandomValues.rfidTagId());
        this.given.rfidTagAssignedToTheWaitingCustomer(rfidTagId);
    }

}
