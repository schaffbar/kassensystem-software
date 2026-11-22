package de.schaffbar.core_pos.domain.customer.web;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import de.schaffbar.core_pos.domain.customer.AgeGroup;
import de.schaffbar.core_pos.domain.customer.CustomerCommands.ChangeCustomerAddressCommand;
import de.schaffbar.core_pos.domain.customer.CustomerCommands.ChangeCustomerContactCommand;
import de.schaffbar.core_pos.domain.customer.CustomerCommands.CreateCustomerCommand;
import de.schaffbar.core_pos.domain.customer.CustomerCommands.UpdateCustomerCommand;
import de.schaffbar.core_pos.domain.customer.CustomerViews.CustomerAddressView;
import de.schaffbar.core_pos.domain.customer.CustomerViews.CustomerView;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ValueObjectMapper;
import de.schaffbar.core_pos.shared.validation.DateOfBirth;
import de.schaffbar.core_pos.shared.validation.PostalCode;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CustomerApiModel extends ValueObjectMapper {

    CustomerApiModel MAPPER = Mappers.getMapper(CustomerApiModel.class);

    // ------------------------------------------------------------------------
    // mapping view to response

    @Mapping(target = "id", source = "id.value")
    CustomerApiDto toCustomerApiDto(CustomerView customer);

    CustomerAddressApiDto toCustomerAddressApiDto(CustomerAddressView address);

    // ------------------------------------------------------------------------
    // mapping request body to command

    CreateCustomerCommand toCreateCustomerCommand(CreateCustomerRequestBody requestBody);

    UpdateCustomerCommand toUpdateCustomerCommand(CustomerId id, UpdateCustomerRequestBody requestBody);

    ChangeCustomerContactCommand toChangeCustomerContactCommand(CustomerId id, ChangeCustomerContactRequestBody requestBody);

    ChangeCustomerAddressCommand toChangeCustomerAddressCommand(CustomerId id, ChangeCustomerAddressRequestBody requestBody);

    // ------------------------------------------------------------------------
    // response

    record CustomerApiDto( //
            @NotBlank String id, //
            @NotBlank String firstName, //
            @NotBlank String lastName, //
            @NotNull LocalDate dateOfBirth, //
            @NotNull AgeGroup ageGroup, //
            boolean clubMember, //
            @NotBlank String email, //
            String phone, //
            @NotNull CustomerAddressApiDto address, //
            @NotNull Instant createdAt, //
            @NotNull Instant updatedAt //
    ) {}

    record CustomerAddressApiDto( //
            @NotBlank String addressLine1, //
            String addressLine2, //
            @NotBlank String postalCode, //
            @NotBlank String city, //
            @NotBlank String country //
    ) {}

    // ------------------------------------------------------------------------
    // request body

    record CreateCustomerRequestBody( //
            @NotBlank String firstName, //
            @NotBlank String lastName, //
            @NotNull @DateOfBirth LocalDate dateOfBirth, //
            boolean clubMember, //
            @NotBlank @Email String email, //
            String phone, //
            @NotBlank String addressLine1, //
            String addressLine2, //
            @NotBlank @PostalCode String postalCode, //
            @NotBlank String city, //
            @NotBlank String country //
    ) {}

    record UpdateCustomerRequestBody( //
            @NotBlank String firstName, //
            @NotBlank String lastName, //
            @NotNull @DateOfBirth LocalDate dateOfBirth, //
            boolean clubMember //
    ) {}

    record ChangeCustomerAddressRequestBody( //
            @NotBlank String addressLine1, //
            String addressLine2, //
            @NotBlank @PostalCode String postalCode, //
            @NotBlank String city, //
            @NotBlank String country //
    ) {}

    record ChangeCustomerContactRequestBody( //
            @NotBlank @Email String email, //
            String phone //
    ) {}

}
