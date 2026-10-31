package de.schaffbar.core_pos.shared.billing;

import static java.util.Objects.isNull;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import lombok.Value;

/**
 * Billing rule for workshop and tool usages: the duration is rounded up to full minutes, each started 6 minutes are
 * one unit.
 */
@Value
public class BillableDuration {

    private static final long SECONDS_PER_MINUTE = 60;

    private static final long MINUTES_PER_UNIT = 6;

    Duration duration;

    private BillableDuration(Duration duration) {
        if (isNull(duration) || duration.isNegative()) {
            throw new IllegalArgumentException("Billable duration must not be null or negative: " + duration);
        }
        this.duration = duration;
    }

    public static BillableDuration of(Duration duration) {
        return new BillableDuration(duration);
    }

    public static BillableDuration between(Instant start, Instant end) {
        return of(Duration.between(start.truncatedTo(ChronoUnit.SECONDS), end.truncatedTo(ChronoUnit.SECONDS)));
    }

    public long getMinutes() {
        return Math.ceilDiv(this.duration.getSeconds(), SECONDS_PER_MINUTE);
    }

    public long getUnits() {
        return Math.ceilDiv(getMinutes(), MINUTES_PER_UNIT);
    }

}
