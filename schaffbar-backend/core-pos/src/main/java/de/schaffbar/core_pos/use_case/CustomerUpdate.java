package de.schaffbar.core_pos.use_case;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.customer.CustomerCommands.UpdateCustomerCommand;
import de.schaffbar.core_pos.domain.customer.CustomerService;
import de.schaffbar.core_pos.shared.cqrs.Command;

@Service
@Validated
@RequiredArgsConstructor
public class CustomerUpdate {

    private final @NonNull CustomerService customerService;

    @Command
    public void process(@NotNull @Valid UpdateCustomerCommand command) {
        this.customerService.updateCustomer(command);
    }

}
