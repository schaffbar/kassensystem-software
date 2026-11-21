package de.schaffbar.core_pos.device;

import static java.util.Map.entry;

import java.util.Map;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import de.schaffbar.core_pos.shared.exception.BusinessRuleViolationException;
import de.schaffbar.core_pos.shared.exception.CustomerAlreadyInWorkshopException;
import de.schaffbar.core_pos.shared.exception.CustomerNotCertifiedForToolException;
import de.schaffbar.core_pos.shared.exception.CustomerNotInWorkshopException;
import de.schaffbar.core_pos.shared.exception.MaxToolUsageExceededException;
import de.schaffbar.core_pos.shared.exception.NoActiveWorkshopUsageFoundException;
import de.schaffbar.core_pos.shared.exception.NoCustomerAssignedException;
import de.schaffbar.core_pos.shared.exception.NoOpenWorkshopSessionException;
import de.schaffbar.core_pos.shared.exception.NoRfidTagAssignmentWaitingException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException.Resource;
import de.schaffbar.core_pos.shared.exception.RfidTagAlreadyRegisteredException;
import de.schaffbar.core_pos.shared.exception.ToolAlreadyInUseByAnotherCustomerException;

// what the display of a reader shows – the device counterpart of shared.errors in the portal
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class DeviceMessages {

    static final String UNEXPECTED = "Unerwarteter Fehler";

    // every code a use case behind /device/card and /device/counter can throw – the tests in integration/device/ show each text
    static final Map<String, String> BY_CODE = Map.ofEntries( //
            entry(NoCustomerAssignedException.CODE, "Kein Kunde für RFID Tag"), //
            entry(CustomerAlreadyInWorkshopException.CODE, "Du schaffst schon"), //
            entry(NoActiveWorkshopUsageFoundException.CODE, "Zwei mal Pause geht nicht"), //
            entry(NoOpenWorkshopSessionException.CODE, "Du warst nie im Werkstatt"), //
            entry(CustomerNotInWorkshopException.CODE, "Du bist nicht in der Werkstatt"), //
            entry(CustomerNotCertifiedForToolException.CODE, "Keine Einweisung"), //
            entry(MaxToolUsageExceededException.CODE, "Max. Werkzeuge erreicht"), //
            entry(ToolAlreadyInUseByAnotherCustomerException.CODE, "Werkzeug belegt"), //
            entry(RfidTagAlreadyRegisteredException.CODE, "RFID bereits vorhanden!"), //
            entry(NoRfidTagAssignmentWaitingException.CODE, "RFID nicht zugewiesen"));

    static String of(RuntimeException e) {
        return switch (e) {
            case BusinessRuleViolationException violation -> BY_CODE.getOrDefault(violation.getCode(), UNEXPECTED);
            case ResourceNotFoundException notFound -> notFound(notFound.getResource());
            default -> UNEXPECTED;
        };
    }

    private static String notFound(Resource resource) {
        return switch (resource) {
            case RFID_READER -> "RFID Reader nicht erkannt";
            case RFID_TAG -> "RFID Tag nicht erkannt";
            case CUSTOMER -> "Kunde nicht erkannt";
            default -> UNEXPECTED;
        };
    }

}
