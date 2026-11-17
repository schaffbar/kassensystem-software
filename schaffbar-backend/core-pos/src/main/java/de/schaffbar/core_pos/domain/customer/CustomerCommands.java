package de.schaffbar.core_pos.domain.customer;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.validation.DateOfBirth;

public interface CustomerCommands {

    int POSTAL_CODE_LENGTH = 5;

    @Builder
    @Validated
    record CreateCustomerCommand( //
            @NotBlank String firstName, //
            @NotBlank String lastName, //
            @NotNull @DateOfBirth LocalDate dateOfBirth, //
            boolean clubMember, //
            @NotBlank @Email String email, //
            String phone, //
            @NotBlank String addressLine1, //
            String addressLine2, //
            @NotBlank @Size(min = POSTAL_CODE_LENGTH, max = POSTAL_CODE_LENGTH) String postalCode, //
            @NotBlank String city, //
            @NotBlank String country //
    ) {}

    @Builder
    @Validated
    record UpdateCustomerCommand( //
            @NotNull @Valid CustomerId id, //
            @NotBlank String firstName, //
            @NotBlank String lastName, //
            @NotNull @DateOfBirth LocalDate dateOfBirth, //
            boolean clubMember //
    ) {}

    @Builder
    @Validated
    record ChangeCustomerContactCommand( //
            @NotNull @Valid CustomerId id, //
            @NotBlank @Email String email, //
            String phone //
    ) {}

    @Builder
    @Validated
    record ChangeCustomerAddressCommand( //
            @NotNull @Valid CustomerId id, //
            @NotBlank String addressLine1, //
            String addressLine2, //
            @NotBlank @Size(min = POSTAL_CODE_LENGTH, max = POSTAL_CODE_LENGTH) String postalCode, //
            @NotBlank String city, //
            @NotBlank String country //
    ) {}

}
