package de.schaffbar.core_pos.domain.customer;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.shared.id.CustomerId;

public interface CustomerCommands {

    @Builder
    @Validated
    record CreateCustomerCommand( //
            @NotBlank String firstName, //
            @NotBlank String lastName, //
            @NotNull LocalDate dateOfBirth, //
            boolean clubMember, //
            @NotBlank String email, //
            String phone, //
            @NotBlank String addressLine1, //
            String addressLine2, //
            @NotBlank String postalCode, //
            @NotBlank String city, //
            @NotBlank String country //
    ) {}

    @Builder
    @Validated
    record UpdateCustomerCommand( //
            @NotNull CustomerId id, //
            @NotBlank String firstName, //
            @NotBlank String lastName, //
            @NotNull LocalDate dateOfBirth, //
            boolean clubMember //
    ) {}

    @Builder
    @Validated
    record UpdateCustomerContactCommand( //
            @NotNull CustomerId id, //
            @NotBlank String email, //
            String phone //
    ) {}

    @Builder
    @Validated
    record UpdateCustomerAddressCommand( //
            @NotNull CustomerId id, //
            @NotBlank String addressLine1, //
            String addressLine2, //
            @NotBlank String postalCode, //
            @NotBlank String city, //
            @NotBlank String country //
    ) {}

}
