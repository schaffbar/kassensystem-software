package de.schaffbar.core_pos.shared.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import de.schaffbar.core_pos.shared.time.WorkshopTime;

class DateOfBirthValidatorTest {

    @Test
    void isValid_lastDayOfLastYear_isAccepted() {
        // given
        int currentYear = WorkshopTime.today().getYear();
        LocalDate dateOfBirth = LocalDate.of(currentYear - 1, 12, 31);

        // when
        boolean valid = isValid(dateOfBirth);

        // then
        assertThat(valid).isTrue();
    }

    @Test
    void isValid_firstDayOfThisYear_isRejected() {
        // given
        int currentYear = WorkshopTime.today().getYear();
        LocalDate dateOfBirth = LocalDate.of(currentYear, 1, 1);

        // when
        boolean valid = isValid(dateOfBirth);

        // then
        assertThat(valid).isFalse();
    }

    @Test
    void isValid_firstDayOf100YearsAgo_isAccepted() {
        // given
        int currentYear = WorkshopTime.today().getYear();
        LocalDate dateOfBirth = LocalDate.of(currentYear - 100, 1, 1);

        // when
        boolean valid = isValid(dateOfBirth);

        // then
        assertThat(valid).isTrue();
    }

    @Test
    void isValid_lastDayOf101YearsAgo_isRejected() {
        // given
        int currentYear = WorkshopTime.today().getYear();
        LocalDate dateOfBirth = LocalDate.of(currentYear - 101, 12, 31);

        // when
        boolean valid = isValid(dateOfBirth);

        // then
        assertThat(valid).isFalse();
    }

    // a missing date is for @NotNull to reject, not for this constraint
    @Test
    void isValid_null_isAccepted() {
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
