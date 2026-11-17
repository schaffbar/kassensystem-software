package de.schaffbar.core_pos.domain.customer;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import de.schaffbar.core_pos.domain.customer.CustomerCommands.ChangeCustomerAddressCommand;
import de.schaffbar.core_pos.domain.customer.CustomerCommands.CreateCustomerCommand;

@Getter
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
class CustomerAddress {

    @NotBlank
    private String addressLine1;

    private String addressLine2;

    @NotBlank
    private String postalCode;

    @NotBlank
    private String city;

    @NotBlank
    private String country;

    static CustomerAddress of(CreateCustomerCommand command) {
        return of(command.addressLine1(), command.addressLine2(), command.postalCode(), command.city(), command.country());
    }

    static CustomerAddress of(ChangeCustomerAddressCommand command) {
        return of(command.addressLine1(), command.addressLine2(), command.postalCode(), command.city(), command.country());
    }

    private static CustomerAddress of(String addressLine1, String addressLine2, String postalCode, String city, String country) {
        CustomerAddress address = new CustomerAddress();
        address.setAddressLine1(addressLine1);
        address.setAddressLine2(addressLine2);
        address.setPostalCode(postalCode);
        address.setCity(city);
        address.setCountry(country);

        return address;
    }

}
