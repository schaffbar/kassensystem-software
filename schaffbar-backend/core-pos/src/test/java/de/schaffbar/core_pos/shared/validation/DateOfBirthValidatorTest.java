package de.schaffbar.core_pos.shared.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Year;

import org.junit.jupiter.api.Test;

class DateOfBirthValidatorTest {

    @Test
    void acceptsTheLastDayOfLastYear() {
        // given
        int currentYear = Year.now().getValue();
        LocalDate dateOfBirth = LocalDate.of(currentYear - 1, 12, 31);

        // when
        boolean valid = isValid(dateOfBirth);

        // then
        assertThat(valid).isTrue();
    }

    @Test
    void rejectsTheFirstDayOfTheCurrentYear() {
        // given
        int currentYear = Year.now().getValue();
        LocalDate dateOfBirth = LocalDate.of(currentYear, 1, 1);

        // when
        boolean valid = isValid(dateOfBirth);

        // then
        assertThat(valid).isFalse();
    }

    @Test
    void acceptsTheFirstDayOfTheYear100YearsAgo() {
        // given
        int currentYear = Year.now().getValue();
        LocalDate dateOfBirth = LocalDate.of(currentYear - 100, 1, 1);

        // when
        boolean valid = isValid(dateOfBirth);

        // then
        assertThat(valid).isTrue();
    }

    @Test
    void rejectsTheLastDayOfTheYear101YearsAgo() {
        // given
        int currentYear = Year.now().getValue();
        LocalDate dateOfBirth = LocalDate.of(currentYear - 101, 12, 31);

        // when
        boolean valid = isValid(dateOfBirth);

        // then
        assertThat(valid).isFalse();
    }

    @Test
    void acceptsNullBecauseNotNullChecksThat() {
        // when
        boolean valid = isValid(null);

        // then
        assertThat(valid).isTrue();
    }

    // ------------------------------------------------------------------------
    // when

    private static boolean isValid(LocalDate dateOfBirth) {
        return new DateOfBirthValidator().isValid(dateOfBirth, null);
    }

}
