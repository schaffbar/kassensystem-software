package de.schaffbar.core_pos.domain.customer;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.shared.id.CustomerId;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CustomerViews {

    CustomerViews MAPPER = Mappers.getMapper(CustomerViews.class);

    // ------------------------------------------------------------------------
    // mapper

    CustomerView toCustomerView(Customer customer);

    // ------------------------------------------------------------------------
    // views

    @Builder
    @Validated
    record CustomerView( //
            @NotNull CustomerId id, //
            @NotBlank String firstName, //
            @NotBlank String lastName, //
            @NotNull LocalDate dateOfBirth, //
            boolean clubMember, //
            @NotBlank String email, //
            String phone, //
            @NotNull CustomerAddressView address, //
            @NotNull Instant createdAt, //
            @NotNull Instant updatedAt //
    ) {

        public String getFullName() {
            return this.firstName + " " + this.lastName;
        }

    }

    @Builder
    @Validated
    record CustomerAddressView( //
            @NotBlank String addressLine1, //
            String addressLine2, //
            @NotBlank String postalCode, //
            @NotBlank String city, //
            @NotBlank String country //
    ) {}

}
