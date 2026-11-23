package de.schaffbar.core_pos.use_case;

import static de.schaffbar.core_pos.builder.CommandBuilder.startToolUsageCommand;
import static de.schaffbar.core_pos.builder.ViewBuilder.customerView;
import static de.schaffbar.core_pos.builder.ViewBuilder.runningWorkshopUsageView;
import static de.schaffbar.core_pos.builder.ViewBuilder.toolView;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.schaffbar.core_pos.domain.customer.CustomerService;
import de.schaffbar.core_pos.domain.tool.CertificationRequirement;
import de.schaffbar.core_pos.domain.tool.ToolService;
import de.schaffbar.core_pos.domain.tool.ToolViews.ToolView;
import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationService;
import de.schaffbar.core_pos.domain.tool_usage.ToolUsageService;
import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageService;
import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageViews.WorkshopUsageView;
import de.schaffbar.core_pos.shared.exception.CustomerNotCertifiedForToolException;
import de.schaffbar.core_pos.shared.exception.CustomerNotInWorkshopException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException.Resource;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;

@ExtendWith(MockitoExtension.class)
class ToolUsageStartTest {

    @Mock
    private CustomerService customerService;

    @Mock
    private ToolService toolService;

    @Mock
    private WorkshopUsageService workshopUsageService;

    @Mock
    private ToolCertificationService toolCertificationService;

    @Mock
    private ToolUsageService toolUsageService;

    @InjectMocks
    private ToolUsageStart toolUsageStart;

    @Test
    void startsTheUsageInTheSessionOfTheRunningWorkshopUsage() {
        // given
        WorkshopUsageView inWorkshop = givenCustomerInTheWorkshop();
        ToolView tool = givenTool(CertificationRequirement.YELLOW);

        // when
        this.toolUsageStart.process(inWorkshop.customerId(), tool.id());

        // then
        expectStartedInTheSessionOf(inWorkshop, tool);
        expectNoCertificationLookedUp();
    }

    @Test
    void startsARedToolForACertifiedCustomer() {
        // given
        WorkshopUsageView inWorkshop = givenCustomerInTheWorkshop();
        ToolView tool = givenTool(CertificationRequirement.RED);
        givenCertification(inWorkshop.customerId(), tool.id());

        // when
        this.toolUsageStart.process(inWorkshop.customerId(), tool.id());

        // then
        expectStartedInTheSessionOf(inWorkshop, tool);
    }

    @Test
    void rejectsARedToolWithoutActiveCertification_beforeLookingForTheWorkshopUsage() {
        // given – checking in would not help, so the missing certification is the answer
        CustomerId customerId = givenCustomer();
        ToolView tool = givenTool(CertificationRequirement.RED);
        givenNoCertification(customerId, tool.id());

        // when
        Throwable thrown = catchThrowable(() -> this.toolUsageStart.process(customerId, tool.id()));

        // then
        assertThat(thrown) //
                .isInstanceOf(CustomerNotCertifiedForToolException.class) //
                .hasFieldOrPropertyWithValue("code", CustomerNotCertifiedForToolException.CODE);
        verifyNoInteractions(this.workshopUsageService);
        expectNothingStarted();
    }

    @Test
    void rejectsACustomerWhoIsNotInTheWorkshop() {
        // given
        CustomerId customerId = givenCustomer();
        ToolView tool = givenTool(CertificationRequirement.YELLOW);
        givenNotInTheWorkshop(customerId);

        // when
        Throwable thrown = catchThrowable(() -> this.toolUsageStart.process(customerId, tool.id()));

        // then
        assertThat(thrown) //
                .isInstanceOf(CustomerNotInWorkshopException.class) //
                .hasFieldOrPropertyWithValue("code", CustomerNotInWorkshopException.CODE);
        expectNothingStarted();
    }

    @Test
    void rejectsAnUnknownTool() {
        // given
        CustomerId customerId = givenCustomer();
        ToolId unknown = givenUnknownTool();

        // when
        Throwable thrown = catchThrowable(() -> this.toolUsageStart.process(customerId, unknown));

        // then
        assertThat(thrown) //
                .isInstanceOf(ResourceNotFoundException.class) //
                .hasFieldOrPropertyWithValue("resource", Resource.TOOL);
        expectNothingStarted();
    }

    @Test
    void rejectsAnUnknownCustomer() {
        // given
        CustomerId unknown = givenUnknownCustomer();

        // when
        Throwable thrown = catchThrowable(() -> this.toolUsageStart.process(unknown, ToolId.random()));

        // then
        assertThat(thrown) //
                .isInstanceOf(ResourceNotFoundException.class) //
                .hasFieldOrPropertyWithValue("resource", Resource.CUSTOMER);
        expectNothingStarted();
    }

    // ------------------------------------------------------------------------
    // given

    private CustomerId givenCustomer() {
        CustomerId customerId = CustomerId.random();
        when(this.customerService.getCustomer(customerId)).thenReturn(Optional.of(customerView(customerId).build()));

        return customerId;
    }

    private CustomerId givenUnknownCustomer() {
        CustomerId unknown = CustomerId.random();
        when(this.customerService.getCustomer(unknown)).thenReturn(Optional.empty());

        return unknown;
    }

    private WorkshopUsageView givenCustomerInTheWorkshop() {
        WorkshopUsageView running = runningWorkshopUsageView().customerId(givenCustomer()).build();
        when(this.workshopUsageService.getRunningWorkshopUsage(running.customerId())).thenReturn(Optional.of(running));

        return running;
    }

    private void givenNotInTheWorkshop(CustomerId customerId) {
        when(this.workshopUsageService.getRunningWorkshopUsage(customerId)).thenReturn(Optional.empty());
    }

    private ToolView givenTool(CertificationRequirement certificationRequirement) {
        ToolView tool = toolView().certificationRequirement(certificationRequirement).build();
        when(this.toolService.getTool(tool.id())).thenReturn(Optional.of(tool));

        return tool;
    }

    private ToolId givenUnknownTool() {
        ToolId unknown = ToolId.random();
        when(this.toolService.getTool(unknown)).thenReturn(Optional.empty());

        return unknown;
    }

    private void givenCertification(CustomerId customerId, ToolId toolId) {
        when(this.toolCertificationService.hasActiveToolCertification(customerId, toolId)).thenReturn(true);
    }

    private void givenNoCertification(CustomerId customerId, ToolId toolId) {
        when(this.toolCertificationService.hasActiveToolCertification(customerId, toolId)).thenReturn(false);
    }

    // ------------------------------------------------------------------------
    // then

    private void expectStartedInTheSessionOf(WorkshopUsageView inWorkshop, ToolView tool) {
        verify(this.toolUsageService).startToolUsage(startToolUsageCommand() //
                .customerId(inWorkshop.customerId()) //
                .toolId(tool.id()) //
                .workshopSessionId(inWorkshop.workshopSessionId()) //
                .build());
    }

    private void expectNoCertificationLookedUp() {
        verifyNoInteractions(this.toolCertificationService);
    }

    private void expectNothingStarted() {
        verifyNoInteractions(this.toolUsageService);
    }

}
