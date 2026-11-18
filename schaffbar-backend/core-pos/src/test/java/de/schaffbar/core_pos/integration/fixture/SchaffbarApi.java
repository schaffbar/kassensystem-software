package de.schaffbar.core_pos.integration.fixture;

import static de.schaffbar.core_pos.builder.CommandBuilder.createCustomerCommand;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import de.schaffbar.core_pos.domain.customer.CustomerCommands.CreateCustomerCommand;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentType;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.IpAddress;
import de.schaffbar.core_pos.shared.id.MacAddress;
import de.schaffbar.core_pos.shared.id.RfidReaderId;
import de.schaffbar.core_pos.shared.id.RfidTagId;
import de.schaffbar.core_pos.shared.id.ToolId;
import de.schaffbar.core_pos.support.Json;

public final class SchaffbarApi {

    private final MockMvcTester mvc;

    SchaffbarApi(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    // ------------------------------------------------------------------------
    // RFID readers

    public MvcTestResult getRfidReaders(MacAddress macAddress) {
        return this.mvc.get().uri("/api/v1/rfid-readers").param("macAddress", macAddress.getValue()).exchange();
    }

    public MvcTestResult getRfidReader(RfidReaderId rfidReaderId) {
        return this.mvc.get().uri("/api/v1/rfid-readers/{rfidReaderId}", rfidReaderId.getValue()).exchange();
    }

    public MvcTestResult createRfidReader(MacAddress macAddress) {
        return createRfidReader(macAddress.getValue());
    }

    // as typed in by hand, also in a format MacAddress does not accept
    public MvcTestResult createRfidReader(String macAddress) {
        return this.mvc.post().uri("/api/v1/rfid-readers") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content("""
                        { "macAddress": "%s" }
                        """.formatted(macAddress)) //
                .exchange();
    }

    public MvcTestResult changeRfidReaderType(RfidReaderId rfidReaderId, RfidReaderType type) {
        return this.mvc.patch().uri("/api/v1/rfid-readers/{rfidReaderId}/type", rfidReaderId.getValue()) //
                .contentType(MediaType.APPLICATION_JSON) //
                .content("""
                        { "type": "%s" }
                        """.formatted(type.name())) //
                .exchange();
    }

    public MvcTestResult deleteRfidReader(RfidReaderId rfidReaderId) {
        return this.mvc.delete().uri("/api/v1/rfid-readers/{rfidReaderId}", rfidReaderId.getValue()).exchange();
    }

    // ------------------------------------------------------------------------
    // RFID tags

    public MvcTestResult getRfidTag(RfidTagId rfidTagId) {
        return this.mvc.get().uri("/api/v1/rfid-tags/{rfidTagId}", rfidTagId.getValue()).exchange();
    }

    public MvcTestResult createRfidTag(RfidTagId rfidTagId) {
        return this.mvc.post().uri("/api/v1/rfid-tags") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content("""
                        { "rfidTagId": "%s" }
                        """.formatted(rfidTagId.getValue())) //
                .exchange();
    }

    public MvcTestResult deleteRfidTag(RfidTagId rfidTagId) {
        return this.mvc.delete().uri("/api/v1/rfid-tags/{rfidTagId}", rfidTagId.getValue()).exchange();
    }

    // ------------------------------------------------------------------------
    // RFID tag assignments

    public MvcTestResult getRfidTagAssignments(CustomerId customerId) {
        return this.mvc.get().uri("/api/v1/rfid-tag-assignments").param("customer-id", customerId.getValue().toString()).exchange();
    }

    public MvcTestResult getRfidTagAssignmentHistory() {
        return this.mvc.get().uri("/api/v1/rfid-tag-assignment-history").exchange();
    }

    public MvcTestResult requestRfidTagAssignment(CustomerId customerId, RfidTagAssignmentType assignmentType) {
        return this.mvc.post().uri("/api/v1/rfid-tag-assignments") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content("""
                        { "customerId": "%s", "assignmentType": "%s" }
                        """.formatted(customerId.getValue(), assignmentType.name())) //
                .exchange();
    }

    public MvcTestResult assignRfidTag(RfidTagId rfidTagId) {
        return this.mvc.put().uri("/api/v1/rfid-tag-assignments/assign") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content("""
                        { "rfidTagId": "%s" }
                        """.formatted(rfidTagId.getValue())) //
                .exchange();
    }

    public MvcTestResult unassignRfidTag(CustomerId customerId) {
        return this.mvc.put().uri("/api/v1/rfid-tag-assignments/unassign") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content("""
                        { "customerId": "%s" }
                        """.formatted(customerId.getValue())) //
                .exchange();
    }

    // ------------------------------------------------------------------------
    // customers

    public MvcTestResult getCustomer(CustomerId customerId) {
        return this.mvc.get().uri("/api/v1/customers/{customerId}", customerId.getValue()).exchange();
    }

    public MvcTestResult createCustomer() {
        return createCustomer(createCustomerCommand().build());
    }

    // the command has the fields of the request body
    public MvcTestResult createCustomer(CreateCustomerCommand command) {
        return this.mvc.post().uri("/api/v1/customers") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content(Json.write(command)) //
                .exchange();
    }

    public MvcTestResult deleteCustomer(CustomerId customerId) {
        return this.mvc.delete().uri("/api/v1/customers/{customerId}", customerId.getValue()).exchange();
    }

    // ------------------------------------------------------------------------
    // tools

    public MvcTestResult getTool(ToolId toolId) {
        return this.mvc.get().uri("/api/v1/tools/{toolId}", toolId.getValue()).exchange();
    }

    public MvcTestResult createTool(String name) {
        return this.mvc.post().uri("/api/v1/tools") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content("""
                        { "name": "%s", "area": "HOLZ", "certificationRequirement": "YELLOW" }
                        """.formatted(name)) //
                .exchange();
    }

    public MvcTestResult updateTool(ToolId toolId, String name) {
        return this.mvc.put().uri("/api/v1/tools/{toolId}", toolId.getValue()) //
                .contentType(MediaType.APPLICATION_JSON) //
                .content("""
                        { "name": "%s", "area": "HOLZ", "certificationRequirement": "YELLOW" }
                        """.formatted(name)) //
                .exchange();
    }

    public MvcTestResult deleteTool(ToolId toolId) {
        return this.mvc.delete().uri("/api/v1/tools/{toolId}", toolId.getValue()).exchange();
    }

    public MvcTestResult assignRfidReader(ToolId toolId, RfidReaderId rfidReaderId) {
        return this.mvc.put().uri("/api/v1/tools/{toolId}/rfid-reader/{rfidReaderId}", toolId.getValue(), rfidReaderId.getValue()).exchange();
    }

    public MvcTestResult clearRfidReader(ToolId toolId) {
        return this.mvc.delete().uri("/api/v1/tools/{toolId}/rfid-reader", toolId.getValue()).exchange();
    }

    public MvcTestResult setWlanRelais(ToolId toolId, IpAddress ipAddress) {
        return this.mvc.put().uri("/api/v1/tools/{toolId}/wlan-relais", toolId.getValue()) //
                .contentType(MediaType.APPLICATION_JSON) //
                .content("""
                        { "wlanRelaisType": "SHELLY_PRO", "ipAddress": "%s" }
                        """.formatted(ipAddress.getValue())) //
                .exchange();
    }

    public MvcTestResult clearWlanRelais(ToolId toolId) {
        return this.mvc.delete().uri("/api/v1/tools/{toolId}/wlan-relais", toolId.getValue()).exchange();
    }

    public MvcTestResult addInstructor(ToolId toolId, CustomerId instructorId) {
        return this.mvc.post().uri("/api/v1/tools/{toolId}/instructors", toolId.getValue()) //
                .contentType(MediaType.APPLICATION_JSON) //
                .content("""
                        { "instructorIds": ["%s"] }
                        """.formatted(instructorId.getValue())) //
                .exchange();
    }

    // ------------------------------------------------------------------------
    // tool certifications

    public MvcTestResult getToolCertifications(ToolId toolId) {
        return this.mvc.get().uri("/api/v1/tool-certifications").param("toolId", toolId.getValue().toString()).exchange();
    }

    public MvcTestResult certifyCustomer(CustomerId customerId, ToolId toolId, CustomerId certifiedBy) {
        return this.mvc.post().uri("/api/v1/tool-certifications/batch") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content("""
                        { "customerIds": ["%s"], "toolId": "%s", "certifiedBy": "%s" }
                        """.formatted(customerId.getValue(), toolId.getValue(), certifiedBy.getValue())) //
                .exchange();
    }

    // ------------------------------------------------------------------------
    // workshop sessions

    public MvcTestResult getOpenWorkshopSession(CustomerId customerId) {
        return this.mvc.get().uri("/api/v1/workshop-sessions/{customerId}/active", customerId.getValue()).exchange();
    }

    public MvcTestResult closeWorkshopSession(CustomerId customerId) {
        return this.mvc.put().uri("/api/v1/workshop-sessions/{customerId}/close", customerId.getValue()).exchange();
    }

    // ------------------------------------------------------------------------
    // devices

    // a device always answers 200 – what happened is in the body and in the data
    public MvcTestResult holdRfidTagToReader(RfidTagId rfidTagId, MacAddress macAddress) {
        return this.mvc.post().uri("/device/card") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content("""
                        { "DEVUSECASE": "test", "MACADDR": "%s", "RFID": "%s" }
                        """.formatted(macAddress.getValue(), rfidTagId.getValue())) //
                .exchange();
    }

}
