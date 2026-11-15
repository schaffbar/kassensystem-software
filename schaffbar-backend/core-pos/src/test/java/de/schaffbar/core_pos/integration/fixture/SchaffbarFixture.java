package de.schaffbar.core_pos.integration.fixture;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

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

}
