package de.schaffbar.core_pos.shared.id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class RfidTagIdTest {

    @ParameterizedTest
    @ValueSource(strings = { "04A31F9C", "04C95A12" })
    void of_tagId_isKept(String tagId) {
        // when
        RfidTagId rfidTagId = RfidTagId.of(tagId);

        // then
        assertThat(rfidTagId.getValue()).isEqualTo(tagId);
    }

    // RT-1
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    void of_blank_isRejected(String blank) {
        // when
        Throwable thrown = catchThrowable(() -> RfidTagId.of(blank));

        // then
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

}
