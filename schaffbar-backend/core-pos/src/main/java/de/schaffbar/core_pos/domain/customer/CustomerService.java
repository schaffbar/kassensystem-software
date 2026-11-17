package de.schaffbar.core_pos.domain.customer;

import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.customer.CustomerCommands.ChangeCustomerAddressCommand;
import de.schaffbar.core_pos.domain.customer.CustomerCommands.ChangeCustomerContactCommand;
import de.schaffbar.core_pos.domain.customer.CustomerCommands.CreateCustomerCommand;
import de.schaffbar.core_pos.domain.customer.CustomerCommands.UpdateCustomerCommand;
import de.schaffbar.core_pos.domain.customer.CustomerViews.CustomerView;
import de.schaffbar.core_pos.shared.cqrs.Command;
import de.schaffbar.core_pos.shared.cqrs.Query;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.event.outbox.OutboxEventStore;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.CustomerId;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class CustomerService {

    private final @NonNull CustomerRepository customerRepository;

    private final @NonNull OutboxEventStore outboxEventStore;

    // ------------------------------------------------------------------------
    // query

    @Query
    public List<CustomerView> getCustomers() {
        return this.customerRepository.findAll().stream() //
                .map(CustomerViews.MAPPER::toCustomerView) //
                .toList();
    }

    @Query
    public Optional<CustomerView> getCustomer(@NotNull @Valid CustomerId id) {
        return this.customerRepository.findById(id.getValue()) //
                .map(CustomerViews.MAPPER::toCustomerView);
    }

    // ------------------------------------------------------------------------
    // command

    @Command
    public CustomerId createCustomer(@NotNull @Valid CreateCustomerCommand command) {
        Customer customer = Customer.of(command);
        Customer savedCustomer = this.customerRepository.save(customer);
        List<SchaffbarEvent> events = List.of(CustomerEventFactory.customerCreated(savedCustomer));

        this.outboxEventStore.save(events);

        log.info("Created customer with id {} and published events {}", savedCustomer.getId(), events);

        return savedCustomer.getId();
    }

    @Command
    public void updateCustomer(@NotNull @Valid UpdateCustomerCommand command) {
        List<SchaffbarEvent> events = this.customerRepository.findById(command.id().getValue()) //
                .map(customer -> customer.update(command)) //
                .orElseThrow(() -> ResourceNotFoundException.customer(command.id()));

        this.outboxEventStore.save(events);

        log.info("Updated customer with id {} and published events {}", command.id(), events);
    }

    @Command
    public void changeCustomerContact(@NotNull @Valid ChangeCustomerContactCommand command) {
        List<SchaffbarEvent> events = this.customerRepository.findById(command.id().getValue()) //
                .map(customer -> customer.changeContact(command)) //
                .orElseThrow(() -> ResourceNotFoundException.customer(command.id()));

        this.outboxEventStore.save(events);

        log.info("Changed contact of customer with id {} and published events {}", command.id(), events);
    }

    @Command
    public void changeCustomerAddress(@NotNull @Valid ChangeCustomerAddressCommand command) {
        List<SchaffbarEvent> events = this.customerRepository.findById(command.id().getValue()) //
                .map(customer -> customer.changeAddress(command)) //
                .orElseThrow(() -> ResourceNotFoundException.customer(command.id()));

        this.outboxEventStore.save(events);

        log.info("Changed address of customer with id {} and published events {}", command.id(), events);
    }

    @Command
    public void deleteCustomer(@NotNull @Valid CustomerId id) {
        Customer customer = this.customerRepository.findById(id.getValue()) //
                .orElseThrow(() -> ResourceNotFoundException.customer(id));

        this.customerRepository.delete(customer);
        List<SchaffbarEvent> events = List.of(CustomerEventFactory.customerDeleted(id));

        this.outboxEventStore.save(events);

        log.info("Deleted customer with id {} and published events {}", id, events);
    }

}
