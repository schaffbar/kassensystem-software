package de.schaffbar.core_pos.shared.event;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ValidationException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import de.schaffbar.core_pos.shared.event.payload.CustomerEventPayload;
import de.schaffbar.core_pos.shared.event.payload.RfidReaderEventPayload;
import de.schaffbar.core_pos.shared.event.payload.RfidTagAssignmentEventPayload;
import de.schaffbar.core_pos.shared.event.payload.RfidTagEventPayload;
import de.schaffbar.core_pos.shared.event.payload.ToolCertificationEventPayload;
import de.schaffbar.core_pos.shared.event.payload.ToolEventPayload;
import de.schaffbar.core_pos.shared.event.payload.ToolUsageEventPayload;
import de.schaffbar.core_pos.shared.event.payload.WorkshopSessionEventPayload;
import de.schaffbar.core_pos.shared.event.payload.WorkshopUsageEventPayload;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.RfidReaderId;
import de.schaffbar.core_pos.shared.id.RfidTagAssignmentId;
import de.schaffbar.core_pos.shared.id.RfidTagId;
import de.schaffbar.core_pos.shared.id.ToolCertificationId;
import de.schaffbar.core_pos.shared.id.ToolId;
import de.schaffbar.core_pos.shared.id.ToolUsageId;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;
import de.schaffbar.core_pos.shared.id.WorkshopUsageId;

@Value
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(Include.NON_NULL)
public class SchaffbarEvent {

    private static final ObjectMapper objectMapper = new ObjectMapper() //
            .registerModule(new JavaTimeModule()) //
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    UUID id;

    EventType type;

    EventVersion version;

    AggregateType aggregateType;

    String aggregateId;

    Instant timestamp;

    JsonNode payload;

    // not set yet – prepared for an outbox relay
    String owner;

    // ------------------------------------------------------------------------
    // static factories

    public static SchaffbarEvent customerEvent(EventType eventType, CustomerId customerId, CustomerEventPayload payload) {
        return builderWithDefaultsAndNow(eventType) //
                .aggregateType(AggregateType.CUSTOMER) //
                .aggregateId(customerId.getValue().toString()) //
                .payload(toJsonNode(payload)) //
                .build();
    }

    public static SchaffbarEvent toolEvent(EventType eventType, ToolId toolId, ToolEventPayload payload) {
        return builderWithDefaultsAndNow(eventType) //
                .aggregateType(AggregateType.TOOL) //
                .aggregateId(toolId.getValue().toString()) //
                .payload(toJsonNode(payload)) //
                .build();
    }

    public static SchaffbarEvent rfidReaderEvent(EventType eventType, RfidReaderId rfidReaderId, RfidReaderEventPayload payload) {
        return builderWithDefaultsAndNow(eventType) //
                .aggregateType(AggregateType.RFID_READER) //
                .aggregateId(rfidReaderId.getValue().toString()) //
                .payload(toJsonNode(payload)) //
                .build();
    }

    public static SchaffbarEvent rfidTagEvent(EventType eventType, RfidTagId rfidTagId, RfidTagEventPayload payload) {
        return builderWithDefaultsAndNow(eventType) //
                .aggregateType(AggregateType.RFID_TAG) //
                .aggregateId(rfidTagId.getValue()) //
                .payload(toJsonNode(payload)) //
                .build();
    }

    public static SchaffbarEvent rfidTagAssignmentEvent(EventType eventType, RfidTagAssignmentId rfidTagAssignmentId, RfidTagAssignmentEventPayload payload) {
        return builderWithDefaultsAndNow(eventType) //
                .aggregateType(AggregateType.RFID_TAG_ASSIGNMENT) //
                .aggregateId(rfidTagAssignmentId.getValue().toString()) //
                .payload(toJsonNode(payload)) //
                .build();
    }

    public static SchaffbarEvent workshopSessionEvent(EventType eventType, WorkshopSessionId workshopSessionId, WorkshopSessionEventPayload payload) {
        return builderWithDefaultsAndNow(eventType) //
                .aggregateType(AggregateType.WORKSHOP_SESSION) //
                .aggregateId(workshopSessionId.getValue().toString()) //
                .payload(toJsonNode(payload)) //
                .build();
    }

    public static SchaffbarEvent workshopUsageEvent(EventType eventType, WorkshopUsageId workshopUsageId, WorkshopUsageEventPayload payload) {
        return builderWithDefaultsAndNow(eventType) //
                .aggregateType(AggregateType.WORKSHOP_USAGE) //
                .aggregateId(workshopUsageId.getValue().toString()) //
                .payload(toJsonNode(payload)) //
                .build();
    }

    public static SchaffbarEvent toolUsageEvent(EventType eventType, ToolUsageId toolUsageId, ToolUsageEventPayload payload) {
        return builderWithDefaultsAndNow(eventType) //
                .aggregateType(AggregateType.TOOL_USAGE) //
                .aggregateId(toolUsageId.getValue().toString()) //
                .payload(toJsonNode(payload)) //
                .build();
    }

    public static SchaffbarEvent toolCertificationEvent(EventType eventType, ToolCertificationId toolCertificationId, ToolCertificationEventPayload payload) {
        return builderWithDefaultsAndNow(eventType) //
                .aggregateType(AggregateType.TOOL_CERTIFICATION) //
                .aggregateId(toolCertificationId.getValue().toString()) //
                .payload(toJsonNode(payload)) //
                .build();
    }

    // ------------------------------------------------------------------------
    // serialization

    public String serialize() {
        try {
            return objectMapper.writeValueAsString(this);
        }
        catch (JsonProcessingException e) {
            throw new ValidationException("Unable to serialize the event", e);
        }
    }

    public <T extends EventPayload> T deserializeAndValidatePayload(Class<T> payloadType) {
        try {
            T payload = objectMapper.treeToValue(this.payload, payloadType);
            payload.validate();
            return payload;
        }
        catch (JsonProcessingException | IllegalArgumentException | ValidationException e) {
            throw new ValidationException("Unable to de-serialize payload", e);
        }
    }

    // ------------------------------------------------------------------------
    // helper

    private static SchaffbarEventBuilder builderWithDefaultsAndNow(EventType eventType) {
        return builderWithDefaults(eventType, Instant.now());
    }

    private static SchaffbarEventBuilder builderWithDefaults(EventType eventType, Instant creationTimestamp) {
        return builder() //
                .id(UUID.randomUUID()) //
                .type(eventType) //
                .version(EventVersion.V1) //
                .timestamp(creationTimestamp);
    }

    private static JsonNode toJsonNode(EventPayload payload) {
        return objectMapper.valueToTree(payload);
    }

    public enum EventVersion {
        V1
    }

}
