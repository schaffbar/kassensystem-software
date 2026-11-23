package de.schaffbar.core_pos.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.ROOT;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.TimeZone;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = ROOT, importOptions = ImportOption.DoNotIncludeTests.class)
class TimeArchitectureTest {

    // the types whose now() without a zone takes the zone of the JVM
    private static final List<Class<?>> ZONED_BY_NOW = List.of( //
            LocalDate.class, LocalDateTime.class, LocalTime.class, ZonedDateTime.class, OffsetDateTime.class, Year.class, YearMonth.class,
            MonthDay.class);

    @ArchTest
    static final ArchRule datesAndTimesAreThoseOfTheWorkshop = noClasses() //
            .should().callMethodWhere(DescribedPredicate.describe("the zone of the JVM (LocalDate.now(), ZoneId.systemDefault(), …)", //
                    TimeArchitectureTest::usesTheZoneOfTheJvm)) //
            .because("the workshop is in Berlin, the server need not be – WorkshopTime.today(), WorkshopTime.ZONE");

    // ------------------------------------------------------------------------
    // helper

    private static boolean usesTheZoneOfTheJvm(JavaMethodCall call) {
        boolean nowWithoutZone = call.getName().equals("now") //
                && call.getTarget().getRawParameterTypes().isEmpty() //
                && ZONED_BY_NOW.stream().anyMatch(type -> call.getTargetOwner().isEquivalentTo(type));
        boolean defaultZone = isCallOf(call, ZoneId.class, "systemDefault") //
                || isCallOf(call, TimeZone.class, "getDefault") //
                || isCallOf(call, Clock.class, "systemDefaultZone");

        return nowWithoutZone || defaultZone;
    }

    private static boolean isCallOf(JavaMethodCall call, Class<?> owner, String name) {
        return call.getTargetOwner().isEquivalentTo(owner) && call.getName().equals(name);
    }

}
