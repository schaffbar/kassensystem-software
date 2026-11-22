package de.schaffbar.core_pos.domain.customer;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.shared.id.CustomerId;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR, imports = LocalDate.class)
public interface CustomerViews {

    CustomerViews MAPPER = Mappers.getMapper(CustomerViews.class);

    // ------------------------------------------------------------------------
    // mapper

    @Mapping(target = "ageGroup", expression = "java(customer.getAgeGroupOn(LocalDate.now()))")
    CustomerView toCustomerView(Customer customer);

    // ------------------------------------------------------------------------
    // views

    @Builder
    @Validated
    record CustomerView( //
            @NotNull @Valid CustomerId id, //
            @NotBlank String firstName, //
            @NotBlank String lastName, //
            @NotNull LocalDate dateOfBirth, //
            @NotNull AgeGroup ageGroup, //
            boolean clubMember, //
            @NotBlank String email, //
            String phone, //
            @NotNull @Valid CustomerAddressView address, //
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
