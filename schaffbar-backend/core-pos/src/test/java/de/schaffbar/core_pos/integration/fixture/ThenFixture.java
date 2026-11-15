package de.schaffbar.core_pos.integration.fixture;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType;
import de.schaffbar.core_pos.shared.id.RfidReaderId;

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

    // ------------------------------------------------------------------------
    // rfid readers

    public void expectRfidReader(RfidReaderId rfidReaderId, RfidReaderType type) {
        MvcTestResult result = this.api.getRfidReader(rfidReaderId);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.type").isEqualTo(type.name());
    }

    public void expectNoRfidReader(RfidReaderId rfidReaderId) {
        expectProblem(this.api.getRfidReader(rfidReaderId), HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    public void expectRfidReaderCount(String macAddress, int count) {
        MvcTestResult result = this.api.getRfidReaders(macAddress);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$").asArray().hasSize(count);
    }

}
