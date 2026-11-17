package de.schaffbar.core_pos.read_facade;

import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.customer.CustomerService;
import de.schaffbar.core_pos.domain.customer.CustomerViews.CustomerView;
import de.schaffbar.core_pos.shared.cqrs.Query;
import de.schaffbar.core_pos.shared.id.CustomerId;

@Service
@Validated
@RequiredArgsConstructor
public class CustomerReadFacade {

    private final @NonNull CustomerService customerService;

    @Query
    public List<CustomerView> getCustomers() {
        return this.customerService.getCustomers();
    }

    @Query
    public Optional<CustomerView> getCustomer(@NotNull @Valid CustomerId customerId) {
        return this.customerService.getCustomer(customerId);
    }

}
