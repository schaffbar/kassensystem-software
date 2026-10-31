package de.schaffbar.core_pos.shared.billing;

import java.util.Collection;

/**
 * Total of several usages. Every usage is rounded on its own, the total is the sum of the rounded values – so it can be
 * more than rounding the summed up duration once.
 */
public record BillingTotal(long minutes, long units) {

    public static final BillingTotal ZERO = new BillingTotal(0, 0);

    public static BillingTotal of(Collection<BillableDuration> durations) {
        return durations.stream() //
                .reduce(ZERO, BillingTotal::plus, BillingTotal::plus);
    }

    public BillingTotal plus(BillableDuration duration) {
        return new BillingTotal(this.minutes + duration.getMinutes(), this.units + duration.getUnits());
    }

    public BillingTotal plus(BillingTotal other) {
        return new BillingTotal(this.minutes + other.minutes, this.units + other.units);
    }

}
