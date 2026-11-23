package de.schaffbar.core_pos.shared.time;

import java.time.LocalDate;
import java.time.ZoneId;

// the workshop is in Germany: its day and the times a device shows are those of Berlin, whatever zone the server
// runs in – stored times stay Instants (UTC)
public final class WorkshopTime {

    public static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

    private WorkshopTime() {}

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

}
