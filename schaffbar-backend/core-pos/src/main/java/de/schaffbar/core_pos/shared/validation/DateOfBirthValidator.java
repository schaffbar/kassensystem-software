package de.schaffbar.core_pos.shared.validation;

import static java.util.Objects.isNull;

import java.time.LocalDate;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DateOfBirthValidator implements ConstraintValidator<DateOfBirth, LocalDate> {

    private static final int MAX_AGE_IN_YEARS = 100;

    @Override
    public boolean isValid(LocalDate dateOfBirth, ConstraintValidatorContext context) {
        if (isNull(dateOfBirth)) {
            return true;
        }

        int currentYear = LocalDate.now().getYear();
        int year = dateOfBirth.getYear();

        return year >= currentYear - MAX_AGE_IN_YEARS && year < currentYear;
    }

}
