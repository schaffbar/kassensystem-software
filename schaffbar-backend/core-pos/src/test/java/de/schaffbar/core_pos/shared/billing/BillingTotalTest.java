package de.schaffbar.core_pos.shared.billing;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class BillingTotalTest {

    @Test
    void isZeroWithoutUsages() {
        assertThat(BillingTotal.of(List.of())).isEqualTo(BillingTotal.ZERO);
    }

    @Test
    void sumsTheRoundedValuesOfEachUsage() {
        // 7 min (2 units) + 31 min (6 units)
        List<BillableDuration> durations = List.of( //
                BillableDuration.of(Duration.ofMinutes(6).plusSeconds(1)), //
                BillableDuration.of(Duration.ofMinutes(30).plusSeconds(10)) //
        );

        assertThat(BillingTotal.of(durations)).isEqualTo(new BillingTotal(38, 8));
    }

    @Test
    void roundsEveryUsageOnItsOwn() {
        // given
        // 3 × 1 s: each counts as 1 min and 1 unit – rounding the summed up 3 s once would give 1 min and 1 unit
        BillableDuration oneSecond = BillableDuration.of(Duration.ofSeconds(1));

        // when
        BillingTotal actual = BillingTotal.of(List.of(oneSecond, oneSecond, oneSecond));

        // then
        assertThat(actual).isEqualTo(new BillingTotal(3, 3));
    }

    @Test
    void addsTwoTotals() {
        // given
        BillingTotal billingTotal1 = new BillingTotal(10, 2);
        BillingTotal billingTotal2 = new BillingTotal(5, 1);

        // when
        BillingTotal actual = billingTotal1.plus(billingTotal2);

        // then
        assertThat(actual).isEqualTo(new BillingTotal(15, 3));
    }

}
