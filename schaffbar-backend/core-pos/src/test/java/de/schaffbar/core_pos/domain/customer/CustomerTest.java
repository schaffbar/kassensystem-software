package de.schaffbar.core_pos.domain.customer;

import static de.schaffbar.core_pos.builder.CommandBuilder.changeCustomerAddressCommand;
import static de.schaffbar.core_pos.builder.CommandBuilder.changeCustomerContactCommand;
import static de.schaffbar.core_pos.builder.CommandBuilder.createCustomerCommand;
import static de.schaffbar.core_pos.builder.CommandBuilder.updateCustomerCommand;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.schaffbar.core_pos.domain.customer.CustomerCommands.ChangeCustomerAddressCommand;
import de.schaffbar.core_pos.domain.customer.CustomerCommands.ChangeCustomerContactCommand;
import de.schaffbar.core_pos.domain.customer.CustomerCommands.CreateCustomerCommand;
import de.schaffbar.core_pos.domain.customer.CustomerCommands.UpdateCustomerCommand;
import de.schaffbar.core_pos.shared.event.EventType;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.event.payload.CustomerEventPayload.CustomerAddressChangedPayload;
import de.schaffbar.core_pos.shared.event.payload.CustomerEventPayload.CustomerContactChangedPayload;
import de.schaffbar.core_pos.shared.event.payload.CustomerEventPayload.CustomerUpdatedPayload;

class CustomerTest {

    // ------------------------------------------------------------------------
    // of

    @Test
    void of_createsTheCustomerWithTheirAddress() {
        // given
        CreateCustomerCommand command = createCustomerCommand() //
                .phone("030 123456") //
                .addressLine2("2. OG") //
                .build();

        // when
        Customer customer = Customer.of(command);

        // then
        assertThat(customer.getId()).isNotNull();
        assertThat(customer.getFirstName()).isEqualTo(command.firstName());
        assertThat(customer.getLastName()).isEqualTo(command.lastName());
        assertThat(customer.getDateOfBirth()).isEqualTo(command.dateOfBirth());
        assertThat(customer.isClubMember()).isEqualTo(command.clubMember());
        assertThat(customer.getEmail()).isEqualTo(command.email());
        assertThat(customer.getPhone()).isEqualTo(command.phone());
        assertThat(customer.getAddress().getAddressLine1()).isEqualTo(command.addressLine1());
        assertThat(customer.getAddress().getAddressLine2()).isEqualTo(command.addressLine2());
        assertThat(customer.getAddress().getPostalCode()).isEqualTo(command.postalCode());
        assertThat(customer.getAddress().getCity()).isEqualTo(command.city());
        assertThat(customer.getAddress().getCountry()).isEqualTo(command.country());
        assertThat(customer.getCreatedAt()).isNotNull();
    }

    // ------------------------------------------------------------------------
    // update

    @Test
    void update_changesTheBaseFieldsAndReturnsTheEvent() {
        // given
        Customer customer = Customer.of(createCustomerCommand().build());
        UpdateCustomerCommand command = updateCustomerCommand(customer.getId()).build();

        // when
        List<SchaffbarEvent> events = customer.update(command);

        // then
        assertThat(customer.getFirstName()).isEqualTo(command.firstName());
        assertThat(customer.getLastName()).isEqualTo(command.lastName());
        assertThat(customer.getDateOfBirth()).isEqualTo(command.dateOfBirth());
        assertThat(customer.isClubMember()).isEqualTo(command.clubMember());

        CustomerUpdatedPayload payload = expectSingleEvent(events, EventType.CUSTOMER_UPDATED, customer) //
                .deserializeAndValidatePayload(CustomerUpdatedPayload.class);
        assertThat(payload.id()).isEqualTo(customer.getId());
        assertThat(payload.firstName()).isEqualTo(command.firstName());
        assertThat(payload.lastName()).isEqualTo(command.lastName());
        assertThat(payload.dateOfBirth()).isEqualTo(command.dateOfBirth());
        assertThat(payload.clubMember()).isEqualTo(command.clubMember());
    }

    // ------------------------------------------------------------------------
    // changeContact

    @Test
    void changeContact_changesEmailAndPhoneAndReturnsTheEvent() {
        // given
        Customer customer = Customer.of(createCustomerCommand().build());
        ChangeCustomerContactCommand command = changeCustomerContactCommand(customer.getId()).build();

        // when
        List<SchaffbarEvent> events = customer.changeContact(command);

        // then
        assertThat(customer.getEmail()).isEqualTo(command.email());
        assertThat(customer.getPhone()).isEqualTo(command.phone());

        CustomerContactChangedPayload payload = expectSingleEvent(events, EventType.CUSTOMER_CONTACT_CHANGED, customer) //
                .deserializeAndValidatePayload(CustomerContactChangedPayload.class);
        assertThat(payload.email()).isEqualTo(command.email());
        assertThat(payload.phone()).isEqualTo(command.phone());
    }

    // ------------------------------------------------------------------------
    // changeAddress

    @Test
    void changeAddress_replacesTheAddressAndReturnsTheEvent() {
        // given
        Customer customer = Customer.of(createCustomerCommand().addressLine2("2. OG").build());
        ChangeCustomerAddressCommand command = changeCustomerAddressCommand(customer.getId()).addressLine2(null).build();

        // when
        List<SchaffbarEvent> events = customer.changeAddress(command);

        // then
        assertThat(customer.getAddress().getAddressLine1()).isEqualTo(command.addressLine1());
        assertThat(customer.getAddress().getAddressLine2()).isNull();
        assertThat(customer.getAddress().getPostalCode()).isEqualTo(command.postalCode());
        assertThat(customer.getAddress().getCity()).isEqualTo(command.city());
        assertThat(customer.getAddress().getCountry()).isEqualTo(command.country());

        CustomerAddressChangedPayload payload = expectSingleEvent(events, EventType.CUSTOMER_ADDRESS_CHANGED, customer) //
                .deserializeAndValidatePayload(CustomerAddressChangedPayload.class);
        assertThat(payload.addressLine1()).isEqualTo(command.addressLine1());
        assertThat(payload.addressLine2()).isNull();
        assertThat(payload.postalCode()).isEqualTo(command.postalCode());
        assertThat(payload.city()).isEqualTo(command.city());
        assertThat(payload.country()).isEqualTo(command.country());
    }

    // ------------------------------------------------------------------------
    // then

    private SchaffbarEvent expectSingleEvent(List<SchaffbarEvent> events, EventType type, Customer customer) {
        assertThat(events).hasSize(1);

        SchaffbarEvent event = events.getFirst();
        assertThat(event.getType()).isEqualTo(type);
        assertThat(event.getAggregateId()).isEqualTo(customer.getId().getValue().toString());

        return event;
    }

}
