package de.schaffbar.core_pos.integration.fixture;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType;
import de.schaffbar.core_pos.domain.rfid_tag_assignment.RfidTagAssignmentType;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.MacAddress;
import de.schaffbar.core_pos.shared.id.RfidReaderId;
import de.schaffbar.core_pos.shared.id.RfidTagId;
import de.schaffbar.core_pos.shared.id.ToolId;

public final class SchaffbarApi {

    private final MockMvcTester mvc;

    SchaffbarApi(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    static UUID createdId(MvcTestResult result) {
        String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
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

    public MvcTestResult createCustomer() {
        return this.mvc.post().uri("/api/v1/customers") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content("""
                        {
                          "firstName": "Erika", "lastName": "Mustermann", "dateOfBirth": "1990-01-31", "clubMember": false,
                          "email": "erika@example.org", "phone": null,
                          "addressLine1": "Hauptstraße 1", "addressLine2": null, "postalCode": "12345", "city": "Berlin", "country": "DE"
                        }
                        """) //
                .exchange();
    }

    // ------------------------------------------------------------------------
    // tools

    public MvcTestResult createTool(String name) {
        return this.mvc.post().uri("/api/v1/tools") //
                .contentType(MediaType.APPLICATION_JSON) //
                .content("""
                        { "name": "%s", "area": "HOLZ", "certificationRequirement": "YELLOW" }
                        """.formatted(name)) //
                .exchange();
    }

    public MvcTestResult assignRfidReader(ToolId toolId, RfidReaderId rfidReaderId) {
        return this.mvc.put().uri("/api/v1/tools/{toolId}/rfid-reader/{rfidReaderId}", toolId.getValue(), rfidReaderId.getValue()).exchange();
    }

    public MvcTestResult clearRfidReader(ToolId toolId) {
        return this.mvc.delete().uri("/api/v1/tools/{toolId}/rfid-reader", toolId.getValue()).exchange();
    }

}
