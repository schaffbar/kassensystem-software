package de.schaffbar.core_pos.web.device;

import static java.util.Objects.isNull;

import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.schaffbar.core_pos.domain.customer.CustomerViews.CustomerView;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderViews.RfidReaderView;
import de.schaffbar.core_pos.domain.tool.ToolViews.ToolView;
import de.schaffbar.core_pos.read_facade.DeviceReadFacade;
import de.schaffbar.core_pos.read_facade.DeviceViews.WorkshopStayView;
import de.schaffbar.core_pos.shared.exception.BusinessRuleViolationException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.MacAddress;
import de.schaffbar.core_pos.shared.id.RfidTagId;
import de.schaffbar.core_pos.use_case.RfidReaderCreate;
import de.schaffbar.core_pos.use_case.RfidTagAssignmentAssignRfidTag;
import de.schaffbar.core_pos.use_case.RfidTagCreate;
import de.schaffbar.core_pos.use_case.ToolUsageToggle;
import de.schaffbar.core_pos.use_case.WorkshopUsageStart;
import de.schaffbar.core_pos.use_case.WorkshopUsageStop;
import de.schaffbar.core_pos.web.device.DeviceApiModel.CounterResponse;
import de.schaffbar.core_pos.web.device.DeviceApiModel.DeviceCardRequestBody;
import de.schaffbar.core_pos.web.device.DeviceApiModel.DeviceCardResponse;
import de.schaffbar.core_pos.web.device.DeviceApiModel.InitRequestBody;
import de.schaffbar.core_pos.web.device.DeviceApiModel.InitResponse;
import de.schaffbar.core_pos.web.device.DeviceApiModel.RfidTagRequestBody;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/device")
public class DeviceController {

    private final @NonNull RfidReaderCreate rfidReaderCreate;

    private final @NonNull RfidTagCreate rfidTagCreate;

    private final @NonNull RfidTagAssignmentAssignRfidTag rfidTagAssignmentAssignRfidTag;

    private final @NonNull WorkshopUsageStart workshopUsageStart;

    private final @NonNull WorkshopUsageStop workshopUsageStop;

    private final @NonNull ToolUsageToggle toolUsageToggle;

    private final @NonNull DeviceReadFacade deviceReadFacade;

    // ------------------------------------------------------------------------
    // command

    @PostMapping(value = "/init", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InitResponse> init(@RequestBody @NotNull InitRequestBody requestBody) {
        log.info("Init RFID reader. Received mac address: {}", requestBody.MACADDR());

        try {
            InitResponse response = initRfidReader(MacAddress.of(requestBody.MACADDR()));

            log.info("RFID reader init response: {}", response);
            return ResponseEntity.ok(response);
        }
        catch (RuntimeException e) {
            log.error("Unexpected error at init of RFID reader [mac address: {}]", requestBody.MACADDR(), e);
            return ResponseEntity.ok(InitResponse.errorWithoutType(DeviceMessages.UNEXPECTED));
        }
    }

    @PostMapping(value = "/counter", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CounterResponse> counter(@RequestBody @NotNull RfidTagRequestBody requestBody) {
        RfidReaderView rfidReader;

        try {
            rfidReader = this.deviceReadFacade.getRfidReader(MacAddress.of(requestBody.MACADDR())).orElse(null);
        }
        catch (RuntimeException e) {
            log.error("Unexpected error at RFID reader [mac address: {}]", requestBody.MACADDR(), e);
            return ResponseEntity.ok(CounterResponse.errorWithoutUseCase(DeviceMessages.UNEXPECTED));
        }

        if (isNull(rfidReader)) {
            log.info("Refused unknown RFID reader [mac address: {}]", requestBody.MACADDR());
            return ResponseEntity.ok(CounterResponse.errorWithoutUseCase(DeviceMessages.UNKNOWN_RFID_READER));
        }

        try {
            CounterResponse response = switch (rfidReader.type()) {
                case RFID_TAG_REGISTER -> registerRfidTag(RfidTagId.of(requestBody.RFID()));
                case RFID_TAG_ASSIGNER -> assignRfidTagOrShowOwner(RfidTagId.of(requestBody.RFID()));
                case GATE_KEEPER_IN, GATE_KEEPER_OUT, SWITCH_BOX -> CounterResponse.errorWithoutUseCase(DeviceMessages.WRONG_RFID_READER);
                case null -> CounterResponse.errorWithoutUseCase(DeviceMessages.RFID_READER_WITHOUT_TYPE);
            };

            return ResponseEntity.ok(response);
        }
        catch (ResourceNotFoundException | BusinessRuleViolationException e) {
            log.info("Refused at RFID reader [id: {}]: {}", rfidReader.id(), e.getMessage());
            return ResponseEntity.ok(CounterResponse.error(rfidReader.type(), DeviceMessages.of(e)));
        }
        catch (RuntimeException e) {
            log.error("Unexpected error at RFID reader [id: {}]", rfidReader.id(), e);
            return ResponseEntity.ok(CounterResponse.error(rfidReader.type(), DeviceMessages.UNEXPECTED));
        }
    }

    @PostMapping(value = "/card", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DeviceCardResponse> card(@RequestBody @NotNull DeviceCardRequestBody requestBody) {
        String devUseCase = requestBody.DEVUSECASE();
        RfidReaderView rfidReader;
        CustomerView customer;

        try {
            rfidReader = this.deviceReadFacade.getRfidReader(MacAddress.of(requestBody.MACADDR())).orElse(null);
            if (isNull(rfidReader)) {
                log.info("Refused unknown RFID reader [mac address: {}]", requestBody.MACADDR());
                return ResponseEntity.ok(DeviceCardResponse.errorNoUserRecognized(DeviceMessages.UNKNOWN_RFID_READER, devUseCase));
            }

            customer = this.deviceReadFacade.getCustomer(RfidTagId.of(requestBody.RFID()));
        }
        catch (ResourceNotFoundException | BusinessRuleViolationException e) {
            log.info("Not recognized at RFID reader [mac address: {}]: {}", requestBody.MACADDR(), e.getMessage());
            return ResponseEntity.ok(DeviceCardResponse.errorNoUserRecognized(DeviceMessages.of(e), devUseCase));
        }
        catch (RuntimeException e) {
            log.error("Unexpected error at RFID reader [mac address: {}]", requestBody.MACADDR(), e);
            return ResponseEntity.ok(DeviceCardResponse.errorUnexpected(DeviceMessages.UNEXPECTED, devUseCase));
        }

        try {
            DeviceCardResponse response = switch (rfidReader.type()) {
                case GATE_KEEPER_IN -> checkIn(customer);
                case GATE_KEEPER_OUT -> checkOut(customer);
                case SWITCH_BOX -> switchTool(customer, rfidReader, devUseCase);
                case RFID_TAG_REGISTER, RFID_TAG_ASSIGNER -> DeviceCardResponse.errorUnexpected(DeviceMessages.WRONG_RFID_READER, devUseCase);
                case null -> DeviceCardResponse.errorUnexpected(DeviceMessages.RFID_READER_WITHOUT_TYPE, devUseCase);
            };

            return ResponseEntity.ok(response);
        }
        catch (BusinessRuleViolationException e) {
            log.info("Refused at RFID reader [id: {}]: {}", rfidReader.id(), e.getMessage());
            return ResponseEntity.ok(DeviceCardResponse.errorNoAccess(DeviceMessages.of(e), customer.getFullName(), devUseCase));
        }
        catch (RuntimeException e) {
            log.error("Unexpected error at RFID reader [id: {}]", rfidReader.id(), e);
            return ResponseEntity.ok(DeviceCardResponse.errorUnexpected(DeviceMessages.UNEXPECTED, devUseCase));
        }
    }

    // ------------------------------------------------------------------------
    // helper

    private InitResponse initRfidReader(MacAddress macAddress) {
        RfidReaderView rfidReader = this.deviceReadFacade.getRfidReader(macAddress).orElse(null);
        if (isNull(rfidReader)) {
            // R-9 – an unknown reader registers itself, without type, until it gets one in the portal
            this.rfidReaderCreate.process(macAddress);
            return InitResponse.errorWithoutType(DeviceMessages.RFID_READER_WITHOUT_TYPE);
        }

        return switch (rfidReader.type()) {
            case SWITCH_BOX -> startSwitchBox(rfidReader);
            case RFID_TAG_REGISTER, RFID_TAG_ASSIGNER, GATE_KEEPER_IN, GATE_KEEPER_OUT -> InitResponse.start(rfidReader.type());
            case null -> InitResponse.errorWithoutType(DeviceMessages.RFID_READER_WITHOUT_TYPE);
        };
    }

    // a switch box only starts with a tool (R-8) that has a WLAN relais (T-23)
    private InitResponse startSwitchBox(RfidReaderView switchBox) {
        ToolView tool = this.deviceReadFacade.getTool(switchBox.id()).orElse(null);
        if (isNull(tool)) {
            return InitResponse.error(RfidReaderType.SWITCH_BOX, DeviceMessages.NO_TOOL);
        }
        // the API sets type and IP address together (T-5), old data may have only one of them
        if (isNull(tool.wlanRelaisType()) || isNull(tool.ipAddress())) {
            return InitResponse.error(RfidReaderType.SWITCH_BOX, DeviceMessages.NO_WLAN_RELAIS);
        }

        return InitResponse.startSwitchBox(tool.ipAddress(), tool.wlanRelaisType());
    }

    private CounterResponse registerRfidTag(RfidTagId rfidTagId) {
        this.rfidTagCreate.process(rfidTagId);

        return CounterResponse.registerOk();
    }

    private CounterResponse assignRfidTagOrShowOwner(RfidTagId rfidTagId) {
        // an assigned tag shows its owner – a waiting request keeps waiting (TA-12)
        if (!this.deviceReadFacade.isRfidTagAssigned(rfidTagId)) {
            this.rfidTagAssignmentAssignRfidTag.process(rfidTagId);
        }

        CustomerView customer = this.deviceReadFacade.getCustomer(rfidTagId);
        return CounterResponse.assignerOk(customer.getFullName(), rfidTagId);
    }

    private DeviceCardResponse checkIn(CustomerView customer) {
        this.workshopUsageStart.process(customer.id());

        WorkshopStayView stay = this.deviceReadFacade.getWorkshopStay(customer.id());
        return DeviceCardResponse.enterOk(customer.getFullName(), stay.finishedUnits(), stay.entryTime());
    }

    private DeviceCardResponse checkOut(CustomerView customer) {
        this.workshopUsageStop.process(customer.id());

        WorkshopStayView stay = this.deviceReadFacade.getWorkshopStay(customer.id());
        return DeviceCardResponse.leaveOk(customer.getFullName(), stay.finishedUnits(), stay.entryTime(), stay.exitTime());
    }

    private DeviceCardResponse switchTool(CustomerView customer, RfidReaderView switchBox, String devUseCase) {
        ToolView tool = this.deviceReadFacade.getTool(switchBox.id()).orElse(null);
        if (isNull(tool)) {
            // R-8 – the tool was taken from the switch box after it started
            log.info("Refused at switch box [id: {}]: no tool assigned", switchBox.id());
            return DeviceCardResponse.errorNoAccess(DeviceMessages.NO_TOOL, customer.getFullName(), devUseCase);
        }

        this.toolUsageToggle.process(customer.id(), tool.id());

        // a use case returns no result – the state after the toggle says whether it switched the tool on or off
        return this.deviceReadFacade.isToolInUse(customer.id(), tool.id()) //
                ? DeviceCardResponse.startToolUsage(customer.getFullName(), tool.name()) //
                : DeviceCardResponse.stopToolUsage(customer.getFullName(), tool.name());
    }

}
