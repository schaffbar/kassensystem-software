package de.schaffbar.core_pos.device;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.time.LocalDateTime;

import jakarta.validation.Valid;
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

import de.schaffbar.core_pos.device.DeviceApiModel.CounterResponse;
import de.schaffbar.core_pos.device.DeviceApiModel.DeviceCardRequestBody;
import de.schaffbar.core_pos.device.DeviceApiModel.DeviceCardResponse;
import de.schaffbar.core_pos.device.DeviceApiModel.InitRequestBody;
import de.schaffbar.core_pos.device.DeviceApiModel.InitResponse;
import de.schaffbar.core_pos.device.DeviceApiModel.RfidTagRequestBody;
import de.schaffbar.core_pos.domain.customer.CustomerViews.CustomerView;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderService;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType;
import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderViews.RfidReaderView;
import de.schaffbar.core_pos.domain.tool.ToolService;
import de.schaffbar.core_pos.domain.tool.ToolViews.ToolView;
import de.schaffbar.core_pos.read_facade.DeviceReadFacade;
import de.schaffbar.core_pos.read_facade.DeviceViews.WorkshopStayView;
import de.schaffbar.core_pos.shared.exception.BusinessRuleViolationException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.MacAddress;
import de.schaffbar.core_pos.shared.id.RfidReaderId;
import de.schaffbar.core_pos.shared.id.RfidTagId;
import de.schaffbar.core_pos.use_case.CustomerAssignRfidTag;
import de.schaffbar.core_pos.use_case.RfidTagCreate;
import de.schaffbar.core_pos.use_case.ToolUsageToggle;
import de.schaffbar.core_pos.use_case.WorkshopUsageStart;
import de.schaffbar.core_pos.use_case.WorkshopUsageStop;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/device")
public class DeviceController {

    private final @NonNull RfidReaderService rfidReaderService;

    private final @NonNull ToolService toolService;

    private final @NonNull RfidTagCreate rfidTagCreate;

    private final @NonNull CustomerAssignRfidTag customerAssignRfidTag;

    private final @NonNull WorkshopUsageStart workshopUsageStart;

    private final @NonNull WorkshopUsageStop workshopUsageStop;

    private final @NonNull ToolUsageToggle toolUsageToggle;

    private final @NonNull DeviceReadFacade deviceReadFacade;


    @PostMapping(value = "/init", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InitResponse> init(@RequestBody @NotNull @Valid InitRequestBody requestBody) {
        log.info("Init RFID reader. Received mac address: {}", requestBody.MACADDR());

        MacAddress macAddress = MacAddress.of(requestBody.MACADDR());
        RfidReaderId id = this.rfidReaderService.getRfidReader(macAddress) //
                .map(RfidReaderView::id) //
                .orElseGet(() -> this.rfidReaderService.createRfidReader(macAddress));

        RfidReaderView rfidReader = this.rfidReaderService.getRfidReader(id) //
                .orElseThrow(() -> ResourceNotFoundException.rfidReader(id));

        ToolView tool = rfidReader.type() == RfidReaderType.SWITCH_BOX ? this.toolService.getTool(rfidReader.id()).orElse(null) : null;
        String error = getInitError(rfidReader, tool);
        // only a switch box that starts gets the commands for the relay of its tool
        boolean relay = error.isEmpty() && nonNull(tool);

        LocalDateTime now = LocalDateTime.now();
        InitResponse response = InitResponse.builder() //
                .STATE(error.isEmpty() ? "START" : "ERROR") //
                .DEVNAME(getDeviceNme(rfidReader)) //
                .STARTHTTP(relay ? "http://" : "") //
                .DEVIP(relay ? tool.ipAddress().getValue() : "") //
                .SWITCHON(relay ? "/relay/0?" + tool.onCommand() : "") //
                .SWITCHOFF(relay ? "/relay/0?" + tool.offCommand() : "") //
                .DEVUSECASE(isNull(rfidReader.type()) ? "ERROR" : rfidReader.type().getKey()) //
                .TERMINAL("") //
                .ERROR(error) //
                .DATEY(now.getYear()) //
                .DATEM(now.getMonthValue()) //
                .DATED(now.getDayOfMonth()) //
                .TIMEH(now.getHour()) //
                .TIMEM(now.getMinute()) //
                .TIMES(now.getSecond()) //
                .build();

        log.info("RFID reader init response: {}", response);

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/counter", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CounterResponse> counter(@RequestBody @NotNull @Valid RfidTagRequestBody requestBody) {
        RfidTagId rfidTagId = RfidTagId.of(requestBody.RFID());
        RfidReaderView rfidReader;

        try {
            rfidReader = this.deviceReadFacade.getRfidReader(MacAddress.of(requestBody.MACADDR()));
        }
        catch (ResourceNotFoundException e) {
            log.error("Not recognized at the counter: {}", e.getMessage());
            return ResponseEntity.ok(CounterResponse.unexpectedRfidReaderType(DeviceMessages.of(e)));
        }

        try {
            CounterResponse response = switch (rfidReader.type()) {
                case RFID_TAG_REGISTER -> registerRfidTag(rfidTagId);
                case RFID_TAG_ASSIGNER -> assignRfidTagOrShowOwner(rfidTagId);
                case GATE_KEEPER_IN, GATE_KEEPER_OUT, SWITCH_BOX -> CounterResponse.unexpectedRfidReaderType("Falsches Lesegerät");
                case null -> CounterResponse.unexpectedRfidReaderType("Lesegerät ohne Typ");
            };

            return ResponseEntity.ok(response);
        }
        catch (ResourceNotFoundException e) {
            log.info("Refused at RFID reader [id: {}]: {}", rfidReader.id(), e.getMessage());
            return ResponseEntity.ok(CounterResponse.error(rfidReader.type(), DeviceMessages.of(e)));
        }
        catch (BusinessRuleViolationException e) {
            log.info("Refused at RFID reader [id: {}]: {}", rfidReader.id(), e.getMessage());
            return ResponseEntity.ok(CounterResponse.error(rfidReader.type(), DeviceMessages.of(e)));
        }
        catch (RuntimeException e) {
            log.error("Unexpected error at RFID reader [id: {}]", rfidReader.id(), e);
            return ResponseEntity.ok(CounterResponse.error(rfidReader.type(), DeviceMessages.UNEXPECTED));
        }
    }

    @PostMapping(value = "/card", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DeviceCardResponse> card(@RequestBody @NotNull @Valid DeviceCardRequestBody requestBody) {
        String devUseCase = requestBody.DEVUSECASE();
        RfidReaderView rfidReader;
        CustomerView customer;

        try {
            rfidReader = this.deviceReadFacade.getRfidReader(MacAddress.of(requestBody.MACADDR()));
            customer = this.deviceReadFacade.getCustomer(RfidTagId.of(requestBody.RFID()));
        }
        catch (ResourceNotFoundException e) {
            log.error("Not recognized at the reader: {}", e.getMessage());
            return ResponseEntity.ok(DeviceCardResponse.errorNoUserRecognized(DeviceMessages.of(e), devUseCase));
        }
        catch (BusinessRuleViolationException e) {
            log.error("Not recognized at the reader: {}", e.getMessage());
            return ResponseEntity.ok(DeviceCardResponse.errorNoUserRecognized(DeviceMessages.of(e), devUseCase));
        }

        try {
            DeviceCardResponse response = switch (rfidReader.type()) {
                case GATE_KEEPER_IN -> checkIn(customer);
                case GATE_KEEPER_OUT -> checkOut(customer);
                case SWITCH_BOX -> switchTool(customer, rfidReader, devUseCase);
                case RFID_TAG_REGISTER, RFID_TAG_ASSIGNER -> DeviceCardResponse.errorUnexpected("Falsches Lesegerät", devUseCase);
                case null -> DeviceCardResponse.errorUnexpected("Lesegerät ohne Typ", devUseCase);
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

    // a switch box can only start with a tool that has a WLAN relais (R-8, T-23); tool is null when none is assigned
    private String getInitError(RfidReaderView rfidReader, ToolView tool) {
        if (isNull(rfidReader.type())) {
            return "Nicht Gefunden";
        }
        if (rfidReader.type() != RfidReaderType.SWITCH_BOX) {
            return "";
        }
        if (isNull(tool)) {
            return "Kein Werkzeug";
        }
        if (isNull(tool.ipAddress())) {
            return "Kein WLAN-Relais";
        }

        return "";
    }

    private String getDeviceNme(RfidReaderView rfidReader) {
        if (isNull(rfidReader.type())) {
            return "ERROR";
        }

        return switch (rfidReader.type()) {
            case RFID_TAG_REGISTER -> "RFID Tag Register";
            case RFID_TAG_ASSIGNER -> "RFID Tag Assigner";
            case GATE_KEEPER_IN -> "Gate Keeper In";
            case GATE_KEEPER_OUT -> "Gate Keeper Out";
            case SWITCH_BOX -> "Switch Box";
        };
    }

    private CounterResponse registerRfidTag(RfidTagId rfidTagId) {
        this.rfidTagCreate.process(rfidTagId);

        return CounterResponse.registerOk();
    }

    private CounterResponse assignRfidTagOrShowOwner(RfidTagId rfidTagId) {
        // an assigned tag shows its owner – a waiting request keeps waiting (TA-2)
        if (!this.deviceReadFacade.isRfidTagAssigned(rfidTagId)) {
            this.customerAssignRfidTag.process(rfidTagId);
        }

        CustomerView customer = this.deviceReadFacade.getCustomer(rfidTagId);
        return CounterResponse.assignerOk(customer.getFullName(), rfidTagId);
    }

    private DeviceCardResponse checkIn(CustomerView customer) {
        this.workshopUsageStart.process(customer.id());

        WorkshopStayView stay = this.deviceReadFacade.getWorkshopStay(customer.id());
        return DeviceCardResponse.enterOk(RfidReaderType.GATE_KEEPER_IN, customer.getFullName(), stay.finishedUnits(), stay.entryTime());
    }

    private DeviceCardResponse checkOut(CustomerView customer) {
        this.workshopUsageStop.process(customer.id());

        WorkshopStayView stay = this.deviceReadFacade.getWorkshopStay(customer.id());
        return DeviceCardResponse.leaveOk(RfidReaderType.GATE_KEEPER_OUT, customer.getFullName(), stay.finishedUnits(), stay.entryTime(),
                stay.exitTime());
    }

    private DeviceCardResponse switchTool(CustomerView customer, RfidReaderView switchBox, String devUseCase) {
        ToolView tool = this.deviceReadFacade.getTool(switchBox.id()).orElse(null);
        if (isNull(tool)) {
            // R-8 – the tool was taken from the switch box after it started
            log.error("No tool assigned to switch box [id: {}]", switchBox.id());
            return DeviceCardResponse.errorNoAccess("Kein Werkzeug", customer.getFullName(), devUseCase);
        }

        this.toolUsageToggle.process(customer.id(), tool.id());

        return this.deviceReadFacade.isToolInUse(customer.id(), tool.id()) //
                ? DeviceCardResponse.startToolUsage(RfidReaderType.SWITCH_BOX, customer.getFullName(), tool.name()) //
                : DeviceCardResponse.stopToolUsage(RfidReaderType.SWITCH_BOX, customer.getFullName(), tool.name());
    }

}
