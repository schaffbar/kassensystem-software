package de.schaffbar.core_pos.web.device;

import static java.util.Map.entry;

import java.util.Map;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import de.schaffbar.core_pos.shared.exception.BusinessRuleViolationException;
import de.schaffbar.core_pos.shared.exception.CustomerAlreadyInWorkshopException;
import de.schaffbar.core_pos.shared.exception.CustomerNotCertifiedForToolException;
import de.schaffbar.core_pos.shared.exception.CustomerNotInWorkshopException;
import de.schaffbar.core_pos.shared.exception.MaxToolUsageExceededException;
import de.schaffbar.core_pos.shared.exception.NoCustomerAssignedException;
import de.schaffbar.core_pos.shared.exception.NoOpenWorkshopSessionException;
import de.schaffbar.core_pos.shared.exception.NoRfidTagAssignmentWaitingException;
import de.schaffbar.core_pos.shared.exception.NoRunningWorkshopUsageFoundException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException.Resource;
import de.schaffbar.core_pos.shared.exception.RfidTagAlreadyRegisteredException;
import de.schaffbar.core_pos.shared.exception.ToolAlreadyInUseByAnotherCustomerException;

// what the display of a reader shows – the device counterpart of shared.errors in the portal
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class DeviceMessages {

    static final String UNEXPECTED = "Unerwarteter Fehler";

    static final String UNKNOWN_RFID_READER = "RFID Reader nicht erkannt";

    static final String RFID_READER_WITHOUT_TYPE = "Lesegerät ohne Typ";

    static final String WRONG_RFID_READER = "Falsches Lesegerät";

    static final String UNKNOWN_RFID_TAG = "RFID Tag nicht erkannt";

    static final String NO_TOOL = "Kein Werkzeug";

    static final String NO_WLAN_RELAIS = "Kein WLAN-Relais";

    // every code a use case behind /device/card and /device/counter can throw – the tests in integration/device/ show each text
    static final Map<String, String> BY_CODE = Map.ofEntries( //
            entry(NoCustomerAssignedException.CODE, "Kein Kunde für RFID Tag"), //
            entry(CustomerAlreadyInWorkshopException.CODE, "Du schaffst schon"), //
            entry(NoRunningWorkshopUsageFoundException.CODE, "Zwei mal Pause geht nicht"), //
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
            case ResourceNotFoundException notFound when notFound.getResource() == Resource.RFID_TAG -> UNKNOWN_RFID_TAG;
            default -> UNEXPECTED;
        };
    }

}
