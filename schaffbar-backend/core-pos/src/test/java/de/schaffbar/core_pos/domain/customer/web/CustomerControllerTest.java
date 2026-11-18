package de.schaffbar.core_pos.domain.customer.web;

import static de.schaffbar.core_pos.builder.CommandBuilder.changeCustomerAddressCommand;
import static de.schaffbar.core_pos.builder.CommandBuilder.changeCustomerContactCommand;
import static de.schaffbar.core_pos.builder.CommandBuilder.createCustomerCommand;
import static de.schaffbar.core_pos.builder.CommandBuilder.updateCustomerCommand;
import static de.schaffbar.core_pos.builder.ViewBuilder.customerView;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import de.schaffbar.core_pos.domain.customer.CustomerCommands.ChangeCustomerAddressCommand;
import de.schaffbar.core_pos.domain.customer.CustomerCommands.ChangeCustomerContactCommand;
import de.schaffbar.core_pos.domain.customer.CustomerCommands.CreateCustomerCommand;
import de.schaffbar.core_pos.domain.customer.CustomerCommands.UpdateCustomerCommand;
import de.schaffbar.core_pos.domain.customer.CustomerViews.CustomerView;
import de.schaffbar.core_pos.read_facade.CustomerReadFacade;
import de.schaffbar.core_pos.shared.exception.CustomerHasOpenWorkshopSessionException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.util.StringToCustomerIdConverter;
import de.schaffbar.core_pos.shared.web.ApiExceptionHandler;
import de.schaffbar.core_pos.support.Json;
import de.schaffbar.core_pos.use_case.CustomerChangeAddress;
import de.schaffbar.core_pos.use_case.CustomerChangeContact;
import de.schaffbar.core_pos.use_case.CustomerCreate;
import de.schaffbar.core_pos.use_case.CustomerDelete;
import de.schaffbar.core_pos.use_case.CustomerUpdate;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @Mock
    private CustomerReadFacade customerReadFacade;

    @Mock
    private CustomerCreate customerCreate;

    @Mock
    private CustomerUpdate customerUpdate;

    @Mock
    private CustomerChangeContact customerChangeContact;

    @Mock
    private CustomerChangeAddress customerChangeAddress;

    @Mock
    private CustomerDelete customerDelete;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        conversionService.addConverter(new StringToCustomerIdConverter());

        CustomerController controller = new CustomerController(this.customerReadFacade, this.customerCreate, this.customerUpdate, this.customerChangeContact,
                this.customerChangeAddress, this.customerDelete);

        this.mockMvc = MockMvcBuilders.standaloneSetup(controller) //
                .setConversionService(conversionService) //
                .setControllerAdvice(new ApiExceptionHandler()) //
                .build();
    }

    // ------------------------------------------------------------------------
    // getCustomers

    @Test
    void getCustomers_returnsAllWithTheirAgeGroup() throws Exception {
        // given
        CustomerView customer = customerView() //
                .dateOfBirth(LocalDate.now().minusYears(10)) //
                .build();
        givenCustomers(customer);

        // when
        ResultActions result = getCustomers();

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(1)) //
                .andExpect(jsonPath("$[0].id").value(customer.id().getValue().toString())) //
                .andExpect(jsonPath("$[0].ageGroup").value("UNDER_16")) //
                .andExpect(jsonPath("$[0].address.city").value(customer.address().city()));
    }

    // ------------------------------------------------------------------------
    // getCustomer

    @Test
    void getCustomer_returnsTheCustomer() throws Exception {
        // given
        CustomerView customer = givenCustomer();

        // when
        ResultActions result = getCustomer(customer.id());

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.id").value(customer.id().getValue().toString())) //
                .andExpect(jsonPath("$.lastName").value(customer.lastName())) //
                .andExpect(jsonPath("$.address.postalCode").value(customer.address().postalCode()));
    }

    @Test
    void getCustomer_unknownId_is404() throws Exception {
        // given
        CustomerId unknown = givenUnknownCustomer();

        // when
        ResultActions result = getCustomer(unknown);

        // then
        result.andExpect(status().isNotFound()) //
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    // ------------------------------------------------------------------------
    // createCustomer

    @Test
    void createCustomer_returns201WithTheLocation() throws Exception {
        // given
        CreateCustomerCommand command = createCustomerCommand().build();
        CustomerId customerId = givenCreatedCustomer(command);

        // when
        ResultActions result = createCustomer(command);

        // then
        result.andExpect(status().isCreated()) //
                .andExpect(header().string("Location", "/api/v1/customers/" + customerId.getValue()));
    }

    @Test
    void createCustomer_withInvalidEmail_is400() throws Exception {
        // given
        CreateCustomerCommand command = createCustomerCommand().email("erika.example.org").build();

        // when
        ResultActions result = createCustomer(command);

        // then
        result.andExpect(status().isBadRequest());
        expectNothingCreated();
    }

    @Test
    void createCustomer_withPostalCodeOfFourCharacters_is400() throws Exception {
        // given
        CreateCustomerCommand command = createCustomerCommand().postalCode("1234").build();

        // when
        ResultActions result = createCustomer(command);

        // then
        result.andExpect(status().isBadRequest());
        expectNothingCreated();
    }

    @Test
    void createCustomer_bornThisYear_is400() throws Exception {
        // given
        CreateCustomerCommand command = createCustomerCommand().dateOfBirth(LocalDate.now().withDayOfYear(1)).build();

        // when
        ResultActions result = createCustomer(command);

        // then
        result.andExpect(status().isBadRequest());
        expectNothingCreated();
    }

    // ------------------------------------------------------------------------
    // updateCustomer

    @Test
    void updateCustomer_passesTheCommandAndIs204() throws Exception {
        // given
        UpdateCustomerCommand command = updateCustomerCommand().build();

        // when
        ResultActions result = updateCustomer(command);

        // then
        result.andExpect(status().isNoContent());
        verify(this.customerUpdate).process(command);
    }

    // ------------------------------------------------------------------------
    // changeCustomerContact

    @Test
    void changeCustomerContact_passesTheCommandAndIs204() throws Exception {
        // given
        ChangeCustomerContactCommand command = changeCustomerContactCommand().build();

        // when
        ResultActions result = changeCustomerContact(command);

        // then
        result.andExpect(status().isNoContent());
        verify(this.customerChangeContact).process(command);
    }

    @Test
    void changeCustomerContact_withInvalidEmail_is400() throws Exception {
        // given
        ChangeCustomerContactCommand command = changeCustomerContactCommand().email("max@").build();

        // when
        ResultActions result = changeCustomerContact(command);

        // then
        result.andExpect(status().isBadRequest());
        verify(this.customerChangeContact, never()).process(any());
    }

    // ------------------------------------------------------------------------
    // changeCustomerAddress

    @Test
    void changeCustomerAddress_passesTheCommandAndIs204() throws Exception {
        // given
        ChangeCustomerAddressCommand command = changeCustomerAddressCommand().build();

        // when
        ResultActions result = changeCustomerAddress(command);

        // then
        result.andExpect(status().isNoContent());
        verify(this.customerChangeAddress).process(command);
    }

    @Test
    void changeCustomerAddress_withPostalCodeOfSixCharacters_is400() throws Exception {
        // given
        ChangeCustomerAddressCommand command = changeCustomerAddressCommand().postalCode("710321").build();

        // when
        ResultActions result = changeCustomerAddress(command);

        // then
        result.andExpect(status().isBadRequest());
        verify(this.customerChangeAddress, never()).process(any());
    }

    // ------------------------------------------------------------------------
    // deleteCustomer

    @Test
    void deleteCustomer_is204() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();

        // when
        ResultActions result = deleteCustomer(customerId);

        // then
        result.andExpect(status().isNoContent());
        verify(this.customerDelete).process(customerId);
    }

    @Test
    void deleteCustomer_withOpenSession_is409WithTheCode() throws Exception {
        // given
        CustomerId customerId = givenCustomerWithOpenSession();

        // when
        ResultActions result = deleteCustomer(customerId);

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(CustomerHasOpenWorkshopSessionException.CODE));
    }

    // ------------------------------------------------------------------------
    // given

    private void givenCustomers(CustomerView... customers) {
        when(this.customerReadFacade.getCustomers()).thenReturn(List.of(customers));
    }

    private CustomerView givenCustomer() {
        CustomerView customer = customerView().build();
        when(this.customerReadFacade.getCustomer(customer.id())).thenReturn(Optional.of(customer));

        return customer;
    }

    private CustomerId givenUnknownCustomer() {
        CustomerId unknown = CustomerId.random();
        when(this.customerReadFacade.getCustomer(unknown)).thenReturn(Optional.empty());

        return unknown;
    }

    private CustomerId givenCreatedCustomer(CreateCustomerCommand command) {
        CustomerId customerId = CustomerId.random();
        when(this.customerCreate.process(command)).thenReturn(customerId);

        return customerId;
    }

    private CustomerId givenCustomerWithOpenSession() {
        CustomerId customerId = CustomerId.random();
        doThrow(new CustomerHasOpenWorkshopSessionException(customerId)).when(this.customerDelete).process(customerId);

        return customerId;
    }

    // ------------------------------------------------------------------------
    // when

    private ResultActions getCustomers() throws Exception {
        return this.mockMvc.perform(get("/api/v1/customers"));
    }

    private ResultActions getCustomer(CustomerId customerId) throws Exception {
        return this.mockMvc.perform(get("/api/v1/customers/" + customerId.getValue()));
    }

    // the commands have the fields of the request bodies; an id in the body is ignored, the path decides
    private ResultActions createCustomer(CreateCustomerCommand command) throws Exception {
        return this.mockMvc.perform(post("/api/v1/customers") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content(Json.write(command)));
    }

    private ResultActions updateCustomer(UpdateCustomerCommand command) throws Exception {
        return this.mockMvc.perform(put("/api/v1/customers/" + command.id().getValue()) //
                .contentType(MediaType.APPLICATION_JSON) //
                .content(Json.write(command)));
    }

    private ResultActions changeCustomerContact(ChangeCustomerContactCommand command) throws Exception {
        return this.mockMvc.perform(put("/api/v1/customers/" + command.id().getValue() + "/contact") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content(Json.write(command)));
    }

    private ResultActions changeCustomerAddress(ChangeCustomerAddressCommand command) throws Exception {
        return this.mockMvc.perform(put("/api/v1/customers/" + command.id().getValue() + "/address") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content(Json.write(command)));
    }

    private ResultActions deleteCustomer(CustomerId customerId) throws Exception {
        return this.mockMvc.perform(delete("/api/v1/customers/" + customerId.getValue()));
    }

    // ------------------------------------------------------------------------
    // then

    private void expectNothingCreated() {
        verify(this.customerCreate, never()).process(any());
    }

}
