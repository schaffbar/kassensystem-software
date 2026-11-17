package de.schaffbar.core_pos.domain.customer.web;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.schaffbar.core_pos.domain.customer.CustomerCommands.ChangeCustomerAddressCommand;
import de.schaffbar.core_pos.domain.customer.CustomerCommands.ChangeCustomerContactCommand;
import de.schaffbar.core_pos.domain.customer.CustomerCommands.CreateCustomerCommand;
import de.schaffbar.core_pos.domain.customer.CustomerCommands.UpdateCustomerCommand;
import de.schaffbar.core_pos.domain.customer.web.CustomerApiModel.ChangeCustomerAddressRequestBody;
import de.schaffbar.core_pos.domain.customer.web.CustomerApiModel.ChangeCustomerContactRequestBody;
import de.schaffbar.core_pos.domain.customer.web.CustomerApiModel.CreateCustomerRequestBody;
import de.schaffbar.core_pos.domain.customer.web.CustomerApiModel.CustomerApiDto;
import de.schaffbar.core_pos.domain.customer.web.CustomerApiModel.UpdateCustomerRequestBody;
import de.schaffbar.core_pos.read_facade.CustomerReadFacade;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.use_case.CustomerChangeAddress;
import de.schaffbar.core_pos.use_case.CustomerChangeContact;
import de.schaffbar.core_pos.use_case.CustomerCreate;
import de.schaffbar.core_pos.use_case.CustomerDelete;
import de.schaffbar.core_pos.use_case.CustomerUpdate;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/v1/customers")
public class CustomerController {

    private final @NonNull CustomerReadFacade customerReadFacade;

    private final @NonNull CustomerCreate customerCreate;

    private final @NonNull CustomerUpdate customerUpdate;

    private final @NonNull CustomerChangeContact customerChangeContact;

    private final @NonNull CustomerChangeAddress customerChangeAddress;

    private final @NonNull CustomerDelete customerDelete;

    // ------------------------------------------------------------------------
    // query

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CustomerApiDto>> getCustomers() {
        List<CustomerApiDto> customers = this.customerReadFacade.getCustomers().stream() //
                .map(CustomerApiModel.MAPPER::toCustomerApiDto) //
                .toList();

        return ResponseEntity.ok(customers);
    }

    @GetMapping(value = "/{customerId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CustomerApiDto> getCustomer(@PathVariable @NotNull @Valid CustomerId customerId) {
        CustomerApiDto customer = this.customerReadFacade.getCustomer(customerId) //
                .map(CustomerApiModel.MAPPER::toCustomerApiDto) //
                .orElseThrow(() -> ResourceNotFoundException.customer(customerId));

        return ResponseEntity.ok(customer);
    }

    // ------------------------------------------------------------------------
    // command

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> createCustomer(@RequestBody @NotNull @Valid CreateCustomerRequestBody requestBody) {
        CreateCustomerCommand command = CustomerApiModel.MAPPER.toCreateCustomerCommand(requestBody);
        CustomerId customerId = this.customerCreate.process(command);
        URI location = URI.create("/api/v1/customers/" + customerId.getValue());

        return ResponseEntity.created(location).build();
    }

    @PutMapping(value = "/{customerId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateCustomer( //
            @PathVariable @NotNull @Valid CustomerId customerId, //
            @RequestBody @NotNull @Valid UpdateCustomerRequestBody requestBody //
    ) {
        UpdateCustomerCommand command = CustomerApiModel.MAPPER.toUpdateCustomerCommand(customerId, requestBody);
        this.customerUpdate.process(command);

        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/{customerId}/contact", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> changeCustomerContact( //
            @PathVariable @NotNull @Valid CustomerId customerId, //
            @RequestBody @NotNull @Valid ChangeCustomerContactRequestBody requestBody //
    ) {
        ChangeCustomerContactCommand command = CustomerApiModel.MAPPER.toChangeCustomerContactCommand(customerId, requestBody);
        this.customerChangeContact.process(command);

        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/{customerId}/address", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> changeCustomerAddress( //
            @PathVariable @NotNull @Valid CustomerId customerId, //
            @RequestBody @NotNull @Valid ChangeCustomerAddressRequestBody requestBody //
    ) {
        ChangeCustomerAddressCommand command = CustomerApiModel.MAPPER.toChangeCustomerAddressCommand(customerId, requestBody);
        this.customerChangeAddress.process(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(value = "/{customerId}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable @NotNull @Valid CustomerId customerId) {
        this.customerDelete.process(customerId);

        return ResponseEntity.noContent().build();
    }

}
