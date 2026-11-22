package de.schaffbar.core_pos.domain.customer;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import de.schaffbar.core_pos.domain.customer.CustomerCommands.ChangeCustomerAddressCommand;
import de.schaffbar.core_pos.domain.customer.CustomerCommands.ChangeCustomerContactCommand;
import de.schaffbar.core_pos.domain.customer.CustomerCommands.CreateCustomerCommand;
import de.schaffbar.core_pos.domain.customer.CustomerCommands.UpdateCustomerCommand;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.id.CustomerId;

@Getter
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@Entity
@Table(name = "CUSTOMER", schema = "SCHAFFBAR")
class Customer {

    @Id
    private UUID id;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotNull
    private LocalDate dateOfBirth;

    private boolean clubMember;

    @NotBlank
    private String email;

    private String phone;

    @NotNull
    private CustomerAddress address;

    @NotNull
    private Instant createdAt;

    @Version
    private Instant updatedAt;

    // ------------------------------------------------------------------------
    // static constructor

    public static Customer of(CreateCustomerCommand command) {
        Customer customer = new Customer();
        customer.setId(CustomerId.random().getValue());
        customer.setFirstName(command.firstName());
        customer.setLastName(command.lastName());
        customer.setDateOfBirth(command.dateOfBirth());
        customer.setClubMember(command.clubMember());
        customer.setEmail(command.email());
        customer.setPhone(command.phone());
        customer.setAddress(CustomerAddress.of(command));
        customer.setCreatedAt(Instant.now());

        return customer;
    }

    // ------------------------------------------------------------------------
    // query

    public CustomerId getId() {
        return CustomerId.of(this.id);
    }

    // C-12 – the day is a parameter, so the age group of any day can be computed (and tested without a clock)
    public AgeGroup getAgeGroupOn(LocalDate day) {
        int age = Period.between(this.dateOfBirth, day).getYears();
        if (age < 16) {
            return AgeGroup.UNDER_16;
        }
        if (age < 18) {
            return AgeGroup.UNDER_18;
        }

        return AgeGroup.ADULT;
    }

    // ------------------------------------------------------------------------
    // command

    public List<SchaffbarEvent> update(UpdateCustomerCommand command) {
        setFirstName(command.firstName());
        setLastName(command.lastName());
        setDateOfBirth(command.dateOfBirth());
        setClubMember(command.clubMember());

        return List.of(CustomerEventFactory.customerUpdated(this));
    }

    public List<SchaffbarEvent> changeContact(ChangeCustomerContactCommand command) {
        setEmail(command.email());
        setPhone(command.phone());

        return List.of(CustomerEventFactory.customerContactChanged(this));
    }

    public List<SchaffbarEvent> changeAddress(ChangeCustomerAddressCommand command) {
        setAddress(CustomerAddress.of(command));

        return List.of(CustomerEventFactory.customerAddressChanged(this));
    }

}
