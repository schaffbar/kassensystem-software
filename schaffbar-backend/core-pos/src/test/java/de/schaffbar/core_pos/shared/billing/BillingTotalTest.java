package de.schaffbar.core_pos.shared.billing;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class BillingTotalTest {

    // ------------------------------------------------------------------------
    // of

    @Test
    void of_noUsages_isZero() {
        // when
        BillingTotal billingTotal = BillingTotal.of(List.of());

        // then
        assertThat(billingTotal).isEqualTo(BillingTotal.ZERO);
    }

    @Test
    void of_sumsTheRoundedValuesOfEachUsage() {
        // given – 7 min (2 units) + 31 min (6 units)
        List<BillableDuration> durations = List.of( //
                BillableDuration.of(Duration.ofMinutes(6).plusSeconds(1)), //
                BillableDuration.of(Duration.ofMinutes(30).plusSeconds(10)) //
        );

        // when
        BillingTotal billingTotal = BillingTotal.of(durations);

        // then
        assertThat(billingTotal).isEqualTo(new BillingTotal(38, 8));
    }

    @Test
    void of_roundsEveryUsageOnItsOwn() {
        // given – 3 × 1 s: each counts as 1 min and 1 unit; rounding the summed up 3 s once would give 1 min and 1 unit
        BillableDuration oneSecond = BillableDuration.of(Duration.ofSeconds(1));

        // when
        BillingTotal billingTotal = BillingTotal.of(List.of(oneSecond, oneSecond, oneSecond));

        // then
        assertThat(billingTotal).isEqualTo(new BillingTotal(3, 3));
    }

    // ------------------------------------------------------------------------
    // plus

    @Test
    void plus_addsMinutesAndUnits() {
        // given
        BillingTotal first = new BillingTotal(10, 2);
        BillingTotal second = new BillingTotal(5, 1);

        // when
        BillingTotal sum = first.plus(second);

        // then
        assertThat(sum).isEqualTo(new BillingTotal(15, 3));
    }

}
