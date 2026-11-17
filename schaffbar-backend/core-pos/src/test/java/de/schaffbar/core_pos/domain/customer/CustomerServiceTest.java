package de.schaffbar.core_pos.domain.customer;

import static de.schaffbar.core_pos.builder.CommandBuilder.changeCustomerAddressCommand;
import static de.schaffbar.core_pos.builder.CommandBuilder.changeCustomerContactCommand;
import static de.schaffbar.core_pos.builder.CommandBuilder.createCustomerCommand;
import static de.schaffbar.core_pos.builder.CommandBuilder.updateCustomerCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.schaffbar.core_pos.domain.customer.CustomerCommands.ChangeCustomerAddressCommand;
import de.schaffbar.core_pos.domain.customer.CustomerCommands.ChangeCustomerContactCommand;
import de.schaffbar.core_pos.domain.customer.CustomerCommands.CreateCustomerCommand;
import de.schaffbar.core_pos.domain.customer.CustomerCommands.UpdateCustomerCommand;
import de.schaffbar.core_pos.shared.event.EventType;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.event.outbox.OutboxEventStore;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.CustomerId;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private OutboxEventStore outboxEventStore;

    @Captor
    private ArgumentCaptor<List<SchaffbarEvent>> events;

    @InjectMocks
    private CustomerService customerService;

    // ------------------------------------------------------------------------
    // createCustomer

    @Test
    void createCustomer_savesTheCustomerAndTheCreatedEvent() {
        // given
        givenSaveReturnsTheCustomer();
        CreateCustomerCommand command = createCustomerCommand().build();

        // when
        CustomerId customerId = this.customerService.createCustomer(command);

        // then
        verify(this.customerRepository).save(any());
        expectSavedEvent(EventType.CUSTOMER_CREATED, customerId);
    }

    // ------------------------------------------------------------------------
    // updateCustomer

    @Test
    void updateCustomer_changesTheCustomerAndSavesTheEvent() {
        // given
        Customer customer = givenCustomer();
        UpdateCustomerCommand command = updateCustomerCommand(customer.getId()).build();

        // when
        this.customerService.updateCustomer(command);

        // then
        assertThat(customer.getFirstName()).isEqualTo(command.firstName());
        expectSavedEvent(EventType.CUSTOMER_UPDATED, customer.getId());
    }

    @Test
    void updateCustomer_rejectsAnUnknownCustomer() {
        // given
        CustomerId unknown = givenUnknownCustomer();
        UpdateCustomerCommand command = updateCustomerCommand(unknown).build();

        // when
        Throwable thrown = catchThrowable(() -> this.customerService.updateCustomer(command));

        // then
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
        expectNoEvent();
    }

    // ------------------------------------------------------------------------
    // changeCustomerContact

    @Test
    void changeCustomerContact_changesTheContactAndSavesTheEvent() {
        // given
        Customer customer = givenCustomer();
        ChangeCustomerContactCommand command = changeCustomerContactCommand(customer.getId()).build();

        // when
        this.customerService.changeCustomerContact(command);

        // then
        assertThat(customer.getEmail()).isEqualTo(command.email());
        expectSavedEvent(EventType.CUSTOMER_CONTACT_CHANGED, customer.getId());
    }

    @Test
    void changeCustomerContact_rejectsAnUnknownCustomer() {
        // given
        CustomerId unknown = givenUnknownCustomer();
        ChangeCustomerContactCommand command = changeCustomerContactCommand(unknown).build();

        // when
        Throwable thrown = catchThrowable(() -> this.customerService.changeCustomerContact(command));

        // then
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
        expectNoEvent();
    }

    // ------------------------------------------------------------------------
    // changeCustomerAddress

    @Test
    void changeCustomerAddress_changesTheAddressAndSavesTheEvent() {
        // given
        Customer customer = givenCustomer();
        ChangeCustomerAddressCommand command = changeCustomerAddressCommand(customer.getId()).build();

        // when
        this.customerService.changeCustomerAddress(command);

        // then
        assertThat(customer.getAddress().getCity()).isEqualTo(command.city());
        expectSavedEvent(EventType.CUSTOMER_ADDRESS_CHANGED, customer.getId());
    }

    @Test
    void changeCustomerAddress_rejectsAnUnknownCustomer() {
        // given
        CustomerId unknown = givenUnknownCustomer();
        ChangeCustomerAddressCommand command = changeCustomerAddressCommand(unknown).build();

        // when
        Throwable thrown = catchThrowable(() -> this.customerService.changeCustomerAddress(command));

        // then
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
        expectNoEvent();
    }

    // ------------------------------------------------------------------------
    // deleteCustomer

    @Test
    void deleteCustomer_deletesTheCustomerAndSavesTheEvent() {
        // given
        Customer customer = givenCustomer();

        // when
        this.customerService.deleteCustomer(customer.getId());

        // then
        verify(this.customerRepository).delete(customer);
        expectSavedEvent(EventType.CUSTOMER_DELETED, customer.getId());
    }

    @Test
    void deleteCustomer_rejectsAnUnknownCustomer() {
        // given
        CustomerId unknown = givenUnknownCustomer();

        // when
        Throwable thrown = catchThrowable(() -> this.customerService.deleteCustomer(unknown));

        // then
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
        verify(this.customerRepository, never()).delete(any());
        expectNoEvent();
    }

    // ------------------------------------------------------------------------
    // given

    private Customer givenCustomer() {
        Customer customer = Customer.of(createCustomerCommand().build());
        when(this.customerRepository.findById(customer.getId().getValue())).thenReturn(Optional.of(customer));

        return customer;
    }

    private CustomerId givenUnknownCustomer() {
        CustomerId unknown = CustomerId.random();
        when(this.customerRepository.findById(unknown.getValue())).thenReturn(Optional.empty());

        return unknown;
    }

    private void givenSaveReturnsTheCustomer() {
        when(this.customerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ------------------------------------------------------------------------
    // then

    private void expectSavedEvent(EventType type, CustomerId customerId) {
        verify(this.outboxEventStore).save(this.events.capture());
        assertThat(this.events.getValue()) //
                .singleElement() //
                .satisfies(event -> {
                    assertThat(event.getType()).isEqualTo(type);
                    assertThat(event.getAggregateId()).isEqualTo(customerId.getValue().toString());
                });
    }

    private void expectNoEvent() {
        verifyNoInteractions(this.outboxEventStore);
    }

}
