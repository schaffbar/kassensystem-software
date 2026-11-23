package de.schaffbar.core_pos.web.device;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import lombok.AccessLevel;
import lombok.Builder;

import de.schaffbar.core_pos.domain.rfid_reader.RfidReaderType;
import de.schaffbar.core_pos.domain.tool.WlanRelaisType;
import de.schaffbar.core_pos.shared.id.IpAddress;
import de.schaffbar.core_pos.shared.id.RfidTagId;

public interface DeviceApiModel {

    // ------------------------------------------------------------------------
    // response

    @Builder(access = AccessLevel.PRIVATE)
    record InitResponse( //
            String STATE,  //
            String DEVNAME,  //
            String STARTHTTP,  //
            String DEVIP,  //
            String SWITCHON,  //
            String SWITCHOFF,  //
            String DEVUSECASE,  //
            String TERMINAL,  //
            String ERROR, //
            int DATEY, //
            int DATEM, //
            int DATED, //
            int TIMEH, //
            int TIMEM, //
            int TIMES //
    ) {

        // a reader without relay: everything except a switch box
        public static InitResponse start(RfidReaderType type) {
            return withCurrentTime() //
                    .STATE("START") //
                    .DEVNAME(deviceName(type)) //
                    .DEVUSECASE(type.getKey()) //
                    .ERROR("") //
                    .build();
        }

        public static InitResponse startSwitchBox(IpAddress relayIpAddress, WlanRelaisType relayType) {
            return withCurrentTime() //
                    .STATE("START") //
                    .DEVNAME(deviceName(RfidReaderType.SWITCH_BOX)) //
                    .DEVUSECASE(RfidReaderType.SWITCH_BOX.getKey()) //
                    .STARTHTTP(relayType.getProtocol() + "://") //
                    .DEVIP(relayIpAddress.getValue()) //
                    .SWITCHON(relayType.getPath() + relayType.getOnCommand()) //
                    .SWITCHOFF(relayType.getPath() + relayType.getOffCommand()) //
                    .ERROR("") //
                    .build();
        }

        public static InitResponse error(RfidReaderType type, String message) {
            return withCurrentTime() //
                    .STATE("ERROR") //
                    .DEVNAME(deviceName(type)) //
                    .DEVUSECASE(type.getKey()) //
                    .ERROR(message) //
                    .build();
        }

        // a reader without type, or one that could not be read
        public static InitResponse errorWithoutType(String message) {
            return withCurrentTime() //
                    .STATE("ERROR") //
                    .DEVNAME("ERROR") //
                    .DEVUSECASE("ERROR") //
                    .ERROR(message) //
                    .build();
        }

        private static InitResponseBuilder withCurrentTime() {
            LocalDateTime now = LocalDateTime.now();

            return InitResponse.builder() //
                    .STARTHTTP("") //
                    .DEVIP("") //
                    .SWITCHON("") //
                    .SWITCHOFF("") //
                    .TERMINAL("") //
                    .DATEY(now.getYear()) //
                    .DATEM(now.getMonthValue()) //
                    .DATED(now.getDayOfMonth()) //
                    .TIMEH(now.getHour()) //
                    .TIMEM(now.getMinute()) //
                    .TIMES(now.getSecond());
        }

        private static String deviceName(RfidReaderType type) {
            return switch (type) {
                case RFID_TAG_REGISTER -> "RFID Tag Register";
                case RFID_TAG_ASSIGNER -> "RFID Tag Assigner";
                case GATE_KEEPER_IN -> "Gate Keeper In";
                case GATE_KEEPER_OUT -> "Gate Keeper Out";
                case SWITCH_BOX -> "Switch Box";
            };
        }

    }

    @Builder(access = AccessLevel.PRIVATE)
    record CounterResponse( //
            String DEVUSECASE,  //
            String ERROR,  //
            String STATE,  //
            String CUSTOMERNAME,  //
            String RFID,  //
            String ICON  //
    ) {

        public static CounterResponse registerOk() {
            return CounterResponse.builder() //
                    .DEVUSECASE(RfidReaderType.RFID_TAG_REGISTER.getKey()) //
                    .STATE("END") //
                    .ERROR("") //
                    .ICON("OK") //
                    .build();
        }

        public static CounterResponse assignerOk(String customerFullName, RfidTagId rfidTagId) {
            return CounterResponse.builder() //
                    .DEVUSECASE(RfidReaderType.RFID_TAG_ASSIGNER.getKey()) //
                    .STATE("END") //
                    .ERROR("") //
                    .ICON("OK") //
                    .CUSTOMERNAME(customerFullName) //
                    .RFID(rfidTagId.getValue()) //
                    .build();
        }

        public static CounterResponse error(RfidReaderType type, String message) {
            return CounterResponse.builder() //
                    .DEVUSECASE(type.getKey()) //
                    .STATE("END") //
                    .ERROR(message) //
                    .CUSTOMERNAME("Error") //
                    .build();
        }

        // a reader that is unknown, has no type or does not belong to the counter
        public static CounterResponse errorWithoutUseCase(String message) {
            return CounterResponse.builder() //
                    .DEVUSECASE("") //
                    .STATE("END") //
                    .ERROR(message) //
                    .CUSTOMERNAME("Error") //
                    .build();
        }

    }

    @Builder(access = AccessLevel.PRIVATE)
    record DeviceCardResponse( //
            String DEVUSECASE,  //
            String ERROR,  //
            String STATE,  //
            String ICON,  //
            String CUSTOMERNAME,  //
            String CUSTOMERSTARTSTOP,  //
            String UNITS  //
    ) {

        public static DeviceCardResponse enterOk(String customerName, long totalUnits, Instant lastEntryTime) {
            LocalTime time = lastEntryTime.atZone(ZoneId.systemDefault()).toLocalTime();
            String entryTime = time.format(DateTimeFormatter.ofPattern("HH:mm"));

            return DeviceCardResponse.builder() //
                    .DEVUSECASE(RfidReaderType.GATE_KEEPER_IN.getKey()) //
                    .ICON("HI") //
                    .ERROR("") //
                    .STATE("END") //
                    .CUSTOMERNAME(customerName) //
                    .CUSTOMERSTARTSTOP(entryTime) //
                    .UNITS(String.valueOf(totalUnits)) //
                    .build();
        }

        public static DeviceCardResponse leaveOk(String customerName, long totalUnits, Instant lastEntryTime, Instant lastExitTime) {
            LocalTime entryTime = lastEntryTime.atZone(ZoneId.systemDefault()).toLocalTime();
            String entryTimeString = entryTime.format(DateTimeFormatter.ofPattern("HH:mm"));

            LocalTime exitTime = lastExitTime.atZone(ZoneId.systemDefault()).toLocalTime();
            String exitTimeString = exitTime.format(DateTimeFormatter.ofPattern("HH:mm"));

            return DeviceCardResponse.builder() //
                    .DEVUSECASE(RfidReaderType.GATE_KEEPER_OUT.getKey()) //
                    .ICON("BYE") //
                    .ERROR("") //
                    .STATE("END") //
                    .CUSTOMERNAME(customerName) //
                    .CUSTOMERSTARTSTOP(entryTimeString + " - " + exitTimeString) //
                    .UNITS(String.valueOf(totalUnits)) //
                    .build();
        }

        public static DeviceCardResponse startToolUsage(String customerName, String toolName) {
            return DeviceCardResponse.builder() //
                    .DEVUSECASE(RfidReaderType.SWITCH_BOX.getKey()) //
                    .ICON("") //
                    .ERROR("") //
                    .STATE("WORKING") //
                    .CUSTOMERNAME(customerName) //
                    .CUSTOMERSTARTSTOP(toolName) //
                    .UNITS("") //
                    .build();
        }

        public static DeviceCardResponse stopToolUsage(String customerName, String toolName) {
            return DeviceCardResponse.builder() //
                    .DEVUSECASE(RfidReaderType.SWITCH_BOX.getKey()) //
                    .ICON("BYE") //
                    .ERROR("") //
                    .STATE("END") //
                    .CUSTOMERNAME(customerName) //
                    .CUSTOMERSTARTSTOP(toolName) //
                    .UNITS("") //
                    .build();
        }

        public static DeviceCardResponse errorNoUserRecognized(String message, String devUseCase) {
            return DeviceCardResponse.builder() //
                    .DEVUSECASE(devUseCase) //
                    .STATE("END") //
                    .ICON("NOREG") //
                    .ERROR(message) //
                    .UNITS("0") //
                    .build();
        }

        public static DeviceCardResponse errorNoAccess(String message, String customerName, String devUseCase) {
            return DeviceCardResponse.builder() //
                    .DEVUSECASE(devUseCase) //
                    .STATE("END") //
                    .ICON("STOP") //
                    .ERROR(message) //
                    .CUSTOMERNAME(customerName) //
                    .UNITS("0") //
                    .build();
        }

        public static DeviceCardResponse errorUnexpected(String message, String devUseCase) {
            return DeviceCardResponse.builder() //
                    .DEVUSECASE(devUseCase) //
                    .STATE("END") //
                    .ICON("NOREG") //
                    .ERROR(message) //
                    .UNITS("0") //
                    .build();
        }

    }

    // ------------------------------------------------------------------------
    // request body

    record InitRequestBody( //
            String MACADDR //
    ) {}

    record RfidTagRequestBody( //
            String MACADDR, //
            String RFID //
    ) {}

    record DeviceCardRequestBody( //
            String DEVUSECASE, //
            String MACADDR, //
            String RFID //
    ) {}

}
