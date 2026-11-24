package de.schaffbar.core_pos.domain.customer.web;

import static de.schaffbar.core_pos.builder.CommandBuilder.changeCustomerAddressCommand;
import static de.schaffbar.core_pos.builder.CommandBuilder.changeCustomerContactCommand;
import static de.schaffbar.core_pos.builder.CommandBuilder.createCustomerCommand;
import static de.schaffbar.core_pos.builder.CommandBuilder.updateCustomerCommand;
import static de.schaffbar.core_pos.builder.ViewBuilder.customerView;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;
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

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException.Resource;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.time.WorkshopTime;
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
                .setMessageConverters(Json.messageConverter()) //
                .setControllerAdvice(new ApiExceptionHandler()) //
                .build();
    }

    // ------------------------------------------------------------------------
    // getCustomers

    @Test
    void getCustomers_returnsAll() throws Exception {
        // given
        CustomerView customer = customerView().phone("030 123456").build();
        givenCustomers(customer);

        // when
        ResultActions result = getCustomers();

        // then
        result.andExpect(status().isOk()) //
                .andExpect(jsonPath("$.length()").value(1)) //
                .andExpect(jsonPath("$[0].id").value(customer.id().getValue().toString())) //
                .andExpect(jsonPath("$[0].firstName").value(customer.firstName())) //
                .andExpect(jsonPath("$[0].lastName").value(customer.lastName())) //
                .andExpect(jsonPath("$[0].dateOfBirth").value(customer.dateOfBirth().toString())) //
                .andExpect(jsonPath("$[0].ageGroup").value(customer.ageGroup().name())) //
                .andExpect(jsonPath("$[0].clubMember").value(customer.clubMember())) //
                .andExpect(jsonPath("$[0].email").value(customer.email())) //
                .andExpect(jsonPath("$[0].phone").value(customer.phone())) //
                .andExpect(jsonPath("$[0].address.addressLine1").value(customer.address().addressLine1())) //
                .andExpect(jsonPath("$[0].address.postalCode").value(customer.address().postalCode())) //
                .andExpect(jsonPath("$[0].address.city").value(customer.address().city())) //
                .andExpect(jsonPath("$[0].address.country").value(customer.address().country())) //
                .andExpect(jsonPath("$[0].createdAt").value(customer.createdAt().toString())) //
                .andExpect(jsonPath("$[0].updatedAt").value(customer.updatedAt().toString()));
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
                .andExpect(jsonPath("$.firstName").value(customer.firstName())) //
                .andExpect(jsonPath("$.lastName").value(customer.lastName())) //
                .andExpect(jsonPath("$.dateOfBirth").value(customer.dateOfBirth().toString())) //
                .andExpect(jsonPath("$.ageGroup").value(customer.ageGroup().name())) //
                .andExpect(jsonPath("$.clubMember").value(customer.clubMember())) //
                .andExpect(jsonPath("$.email").value(customer.email())) //
                .andExpect(jsonPath("$.address.addressLine1").value(customer.address().addressLine1())) //
                .andExpect(jsonPath("$.address.postalCode").value(customer.address().postalCode())) //
                .andExpect(jsonPath("$.address.city").value(customer.address().city())) //
                .andExpect(jsonPath("$.address.country").value(customer.address().country())) //
                .andExpect(jsonPath("$.createdAt").value(customer.createdAt().toString())) //
                .andExpect(jsonPath("$.updatedAt").value(customer.updatedAt().toString()));
    }

    @Test
    void getCustomer_unknownId_is404() throws Exception {
        // given
        CustomerId unknown = givenUnknownCustomer();

        // when
        ResultActions result = getCustomer(unknown);

        // then
        expectCustomerNotFound(result);
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

    @ParameterizedTest
    @MethodSource("invalidCreateCustomerCommands")
    void createCustomer_withAnInvalidValue_is400(CreateCustomerCommand command) throws Exception {
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
        expectUpdated(command);
    }

    @ParameterizedTest
    @MethodSource("invalidUpdateCustomerCommands")
    void updateCustomer_withAnInvalidValue_is400(UpdateCustomerCommand command) throws Exception {
        // when
        ResultActions result = updateCustomer(command);

        // then
        result.andExpect(status().isBadRequest());
        expectNothingUpdated();
    }

    @Test
    void updateCustomer_unknownId_is404() throws Exception {
        // given
        UpdateCustomerCommand command = updateCustomerCommand().build();
        givenUpdateRejectedWith(command, ResourceNotFoundException.customer(command.id()));

        // when
        ResultActions result = updateCustomer(command);

        // then
        expectCustomerNotFound(result);
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
        expectContactChanged(command);
    }

    @ParameterizedTest
    @MethodSource("invalidChangeCustomerContactCommands")
    void changeCustomerContact_withAnInvalidValue_is400(ChangeCustomerContactCommand command) throws Exception {
        // when
        ResultActions result = changeCustomerContact(command);

        // then
        result.andExpect(status().isBadRequest());
        expectContactNotChanged();
    }

    @Test
    void changeCustomerContact_unknownId_is404() throws Exception {
        // given
        ChangeCustomerContactCommand command = changeCustomerContactCommand().build();
        givenChangeContactRejectedWith(command, ResourceNotFoundException.customer(command.id()));

        // when
        ResultActions result = changeCustomerContact(command);

        // then
        expectCustomerNotFound(result);
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
        expectAddressChanged(command);
    }

    @ParameterizedTest
    @MethodSource("invalidChangeCustomerAddressCommands")
    void changeCustomerAddress_withAnInvalidValue_is400(ChangeCustomerAddressCommand command) throws Exception {
        // when
        ResultActions result = changeCustomerAddress(command);

        // then
        result.andExpect(status().isBadRequest());
        expectAddressNotChanged();
    }

    @Test
    void changeCustomerAddress_unknownId_is404() throws Exception {
        // given
        ChangeCustomerAddressCommand command = changeCustomerAddressCommand().build();
        givenChangeAddressRejectedWith(command, ResourceNotFoundException.customer(command.id()));

        // when
        ResultActions result = changeCustomerAddress(command);

        // then
        expectCustomerNotFound(result);
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
        expectDeleted(customerId);
    }

    @Test
    void deleteCustomer_withOpenSession_is409WithTheCode() throws Exception {
        // given
        CustomerId customerId = CustomerId.random();
        givenDeleteRejectedWith(customerId, new CustomerHasOpenWorkshopSessionException(customerId));

        // when
        ResultActions result = deleteCustomer(customerId);

        // then
        result.andExpect(status().isConflict()) //
                .andExpect(jsonPath("$.code").value(CustomerHasOpenWorkshopSessionException.CODE));
    }

    @Test
    void deleteCustomer_unknownId_is404() throws Exception {
        // given
        CustomerId unknown = CustomerId.random();
        givenDeleteRejectedWith(unknown, ResourceNotFoundException.customer(unknown));

        // when
        ResultActions result = deleteCustomer(unknown);

        // then
        expectCustomerNotFound(result);
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

    private void givenUpdateRejectedWith(UpdateCustomerCommand command, RuntimeException rejection) {
        doThrow(rejection).when(this.customerUpdate).process(command);
    }

    private void givenChangeContactRejectedWith(ChangeCustomerContactCommand command, RuntimeException rejection) {
        doThrow(rejection).when(this.customerChangeContact).process(command);
    }

    private void givenChangeAddressRejectedWith(ChangeCustomerAddressCommand command, RuntimeException rejection) {
        doThrow(rejection).when(this.customerChangeAddress).process(command);
    }

    private void givenDeleteRejectedWith(CustomerId customerId, RuntimeException rejection) {
        doThrow(rejection).when(this.customerDelete).process(customerId);
    }

    // C-1, C-2, C-3, C-6, C-7, C-8 – each format rule of the request body once
    static Stream<Arguments> invalidCreateCustomerCommands() {
        return Stream.of( //
                arguments(named("blank first name (C-1)", createCustomerCommand().firstName(" ").build())), //
                arguments(named("blank last name (C-1)", createCustomerCommand().lastName(" ").build())), //
                arguments(named("blank email (C-1)", createCustomerCommand().email(" ").build())), //
                arguments(named("no date of birth (C-2)", createCustomerCommand().dateOfBirth(null).build())), //
                arguments(named("blank address line (C-3)", createCustomerCommand().addressLine1(" ").build())), //
                arguments(named("blank city (C-3)", createCustomerCommand().city(" ").build())), //
                arguments(named("blank country (C-3)", createCustomerCommand().country(" ").build())), //
                arguments(named("invalid email (C-6)", createCustomerCommand().email("erika.example.org").build())), //
                arguments(named("postal code of four characters (C-7)", createCustomerCommand().postalCode("1234").build())), //
                arguments(named("born this year (C-8)", createCustomerCommand().dateOfBirth(WorkshopTime.today().withDayOfYear(1)).build())));
    }

    static Stream<Arguments> invalidUpdateCustomerCommands() {
        return Stream.of( //
                arguments(named("blank first name (C-1)", updateCustomerCommand().firstName(" ").build())), //
                arguments(named("blank last name (C-1)", updateCustomerCommand().lastName(" ").build())), //
                arguments(named("no date of birth (C-2)", updateCustomerCommand().dateOfBirth(null).build())), //
                arguments(named("born this year (C-8)", updateCustomerCommand().dateOfBirth(WorkshopTime.today().withDayOfYear(1)).build())), //
                arguments(named("born more than 100 years ago (C-8)", updateCustomerCommand().dateOfBirth(WorkshopTime.today().minusYears(101)).build())));
    }

    static Stream<Arguments> invalidChangeCustomerContactCommands() {
        return Stream.of( //
                arguments(named("blank email (C-1)", changeCustomerContactCommand().email(" ").build())), //
                arguments(named("invalid email (C-6)", changeCustomerContactCommand().email("max@").build())));
    }

    static Stream<Arguments> invalidChangeCustomerAddressCommands() {
        return Stream.of( //
                arguments(named("blank address line (C-3)", changeCustomerAddressCommand().addressLine1(" ").build())), //
                arguments(named("blank city (C-3)", changeCustomerAddressCommand().city(" ").build())), //
                arguments(named("blank country (C-3)", changeCustomerAddressCommand().country(" ").build())), //
                arguments(named("postal code of six characters (C-7)", changeCustomerAddressCommand().postalCode("710321").build())));
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

    private void expectCustomerNotFound(ResultActions result) throws Exception {
        result.andExpect(status().isNotFound()) //
                .andExpect(jsonPath("$.code").value(ResourceNotFoundException.CODE)) //
                .andExpect(jsonPath("$.resource").value(Resource.CUSTOMER.name()));
    }

    private void expectNothingCreated() {
        verify(this.customerCreate, never()).process(any());
    }

    private void expectUpdated(UpdateCustomerCommand command) {
        verify(this.customerUpdate).process(command);
    }

    private void expectNothingUpdated() {
        verify(this.customerUpdate, never()).process(any());
    }

    private void expectContactChanged(ChangeCustomerContactCommand command) {
        verify(this.customerChangeContact).process(command);
    }

    private void expectContactNotChanged() {
        verify(this.customerChangeContact, never()).process(any());
    }

    private void expectAddressChanged(ChangeCustomerAddressCommand command) {
        verify(this.customerChangeAddress).process(command);
    }

    private void expectAddressNotChanged() {
        verify(this.customerChangeAddress, never()).process(any());
    }

    private void expectDeleted(CustomerId customerId) {
        verify(this.customerDelete).process(customerId);
    }

}
