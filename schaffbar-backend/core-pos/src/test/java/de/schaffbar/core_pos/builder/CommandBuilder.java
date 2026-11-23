package de.schaffbar.core_pos.builder;

import java.time.LocalDate;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import de.schaffbar.core_pos.domain.customer.CustomerCommands.ChangeCustomerAddressCommand;
import de.schaffbar.core_pos.domain.customer.CustomerCommands.ChangeCustomerAddressCommand.ChangeCustomerAddressCommandBuilder;
import de.schaffbar.core_pos.domain.customer.CustomerCommands.ChangeCustomerContactCommand;
import de.schaffbar.core_pos.domain.customer.CustomerCommands.ChangeCustomerContactCommand.ChangeCustomerContactCommandBuilder;
import de.schaffbar.core_pos.domain.customer.CustomerCommands.CreateCustomerCommand;
import de.schaffbar.core_pos.domain.customer.CustomerCommands.CreateCustomerCommand.CreateCustomerCommandBuilder;
import de.schaffbar.core_pos.domain.customer.CustomerCommands.UpdateCustomerCommand;
import de.schaffbar.core_pos.domain.customer.CustomerCommands.UpdateCustomerCommand.UpdateCustomerCommandBuilder;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderCommands.ChangeRfidReaderTypeCommand;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderCommands.ChangeRfidReaderTypeCommand.ChangeRfidReaderTypeCommandBuilder;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderCommands.UpdateRfidReaderCommand;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderCommands.UpdateRfidReaderCommand.UpdateRfidReaderCommandBuilder;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType;
import de.schaffbar.core_pos.domain.tool.CertificationRequirement;
import de.schaffbar.core_pos.domain.tool.ToolArea;
import de.schaffbar.core_pos.domain.tool.ToolCommands.CreateToolCommand;
import de.schaffbar.core_pos.domain.tool.ToolCommands.CreateToolCommand.CreateToolCommandBuilder;
import de.schaffbar.core_pos.domain.tool.ToolCommands.SetWlanRelaisCommand;
import de.schaffbar.core_pos.domain.tool.ToolCommands.SetWlanRelaisCommand.SetWlanRelaisCommandBuilder;
import de.schaffbar.core_pos.domain.tool.ToolCommands.UpdateToolCommand;
import de.schaffbar.core_pos.domain.tool.ToolCommands.UpdateToolCommand.UpdateToolCommandBuilder;
import de.schaffbar.core_pos.domain.tool.WlanRelaisType;
import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationCommands.CreateToolCertificationCommand;
import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationCommands.CreateToolCertificationCommand.CreateToolCertificationCommandBuilder;
import de.schaffbar.core_pos.domain.tool_usage.ToolUsageCommands.StartToolUsageCommand;
import de.schaffbar.core_pos.domain.tool_usage.ToolUsageCommands.StartToolUsageCommand.StartToolUsageCommandBuilder;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionCommands.CloseWorkshopSessionCommand;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionCommands.CloseWorkshopSessionCommand.CloseWorkshopSessionCommandBuilder;
import de.schaffbar.core_pos.shared.billing.BillingTotal;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.RfidReaderId;
import de.schaffbar.core_pos.shared.id.ToolId;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommandBuilder {

    // ------------------------------------------------------------------------
    // customer

    public static CreateCustomerCommandBuilder createCustomerCommand() {
        return CreateCustomerCommand.builder() //
                .firstName("Erika") //
                .lastName("Mustermann") //
                .dateOfBirth(LocalDate.of(1990, 1, 31)) //
                .clubMember(false) //
                .email("erika@example.org") //
                .addressLine1("Hauptstraße 1") //
                .postalCode("12345") //
                .city("Berlin") //
                .country("DE");
    }

    public static UpdateCustomerCommandBuilder updateCustomerCommand() {
        return updateCustomerCommand(CustomerId.random());
    }

    public static UpdateCustomerCommandBuilder updateCustomerCommand(CustomerId id) {
        return UpdateCustomerCommand.builder() //
                .id(id) //
                .firstName("Max") //
                .lastName("Musterfrau") //
                .dateOfBirth(LocalDate.of(1985, 5, 20)) //
                .clubMember(true);
    }

    public static ChangeCustomerContactCommandBuilder changeCustomerContactCommand() {
        return changeCustomerContactCommand(CustomerId.random());
    }

    public static ChangeCustomerContactCommandBuilder changeCustomerContactCommand(CustomerId id) {
        return ChangeCustomerContactCommand.builder() //
                .id(id) //
                .email("max@example.org") //
                .phone("0170 1234567");
    }

    public static ChangeCustomerAddressCommandBuilder changeCustomerAddressCommand() {
        return changeCustomerAddressCommand(CustomerId.random());
    }

    public static ChangeCustomerAddressCommandBuilder changeCustomerAddressCommand(CustomerId id) {
        return ChangeCustomerAddressCommand.builder() //
                .id(id) //
                .addressLine1("Bahnhofstraße 2") //
                .addressLine2("Hinterhaus") //
                .postalCode("71032") //
                .city("Böblingen") //
                .country("Deutschland");
    }

    // ------------------------------------------------------------------------
    // RFID reader

    public static UpdateRfidReaderCommandBuilder updateRfidReaderCommand() {
        return updateRfidReaderCommand(RfidReaderId.random());
    }

    public static UpdateRfidReaderCommandBuilder updateRfidReaderCommand(RfidReaderId id) {
        return UpdateRfidReaderCommand.builder() //
                .id(id) //
                .name("Theke") //
                .socketName("socket-43");
    }

    public static ChangeRfidReaderTypeCommandBuilder changeRfidReaderTypeCommand() {
        return changeRfidReaderTypeCommand(RfidReaderId.random());
    }

    public static ChangeRfidReaderTypeCommandBuilder changeRfidReaderTypeCommand(RfidReaderId id) {
        return ChangeRfidReaderTypeCommand.builder() //
                .id(id) //
                .type(RfidReaderType.SWITCH_BOX);
    }

    // ------------------------------------------------------------------------
    // tool

    public static CreateToolCommandBuilder createToolCommand() {
        return CreateToolCommand.builder() //
                .name(RandomValues.toolName()) //
                .description("Für Längs- und Querschnitte") //
                .area(ToolArea.HOLZ) //
                .certificationRequirement(CertificationRequirement.RED);
    }

    public static UpdateToolCommandBuilder updateToolCommand() {
        return updateToolCommand(ToolId.random());
    }

    public static UpdateToolCommandBuilder updateToolCommand(ToolId id) {
        return UpdateToolCommand.builder() //
                .id(id) //
                .name(RandomValues.toolName()) //
                .description("Mit Absaugung") //
                .area(ToolArea.METALL) //
                .certificationRequirement(CertificationRequirement.YELLOW);
    }

    public static SetWlanRelaisCommandBuilder setWlanRelaisCommand() {
        return setWlanRelaisCommand(ToolId.random());
    }

    public static SetWlanRelaisCommandBuilder setWlanRelaisCommand(ToolId id) {
        return SetWlanRelaisCommand.builder() //
                .id(id) //
                .wlanRelaisType(WlanRelaisType.SHELLY_PRO) //
                .ipAddress(RandomValues.ipAddress());
    }

    // ------------------------------------------------------------------------
    // tool certification

    public static CreateToolCertificationCommandBuilder createToolCertificationCommand() {
        return CreateToolCertificationCommand.builder() //
                .customerId(CustomerId.random()) //
                .toolId(ToolId.random()) //
                .certifiedBy(CustomerId.random());
    }

    // ------------------------------------------------------------------------
    // tool usage

    public static StartToolUsageCommandBuilder startToolUsageCommand() {
        return StartToolUsageCommand.builder() //
                .customerId(CustomerId.random()) //
                .toolId(ToolId.random()) //
                .workshopSessionId(WorkshopSessionId.random());
    }

    // ------------------------------------------------------------------------
    // workshop session

    public static CloseWorkshopSessionCommandBuilder closeWorkshopSessionCommand() {
        return closeWorkshopSessionCommand(WorkshopSessionId.random());
    }

    public static CloseWorkshopSessionCommandBuilder closeWorkshopSessionCommand(WorkshopSessionId id) {
        return CloseWorkshopSessionCommand.builder() //
                .id(id) //
                .workshopTotal(new BillingTotal(31, 6)) //
                .toolTotal(new BillingTotal(7, 2));
    }

}
