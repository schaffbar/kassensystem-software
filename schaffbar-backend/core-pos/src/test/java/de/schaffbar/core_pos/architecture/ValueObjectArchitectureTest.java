package de.schaffbar.core_pos.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.ROOT;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.classCondition;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.hasRandom;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import de.schaffbar.core_pos.shared.id.ValueObject;
import de.schaffbar.core_pos.shared.id.ValueObjectAssert;

@AnalyzeClasses(packages = ROOT, importOptions = ImportOption.DoNotIncludeTests.class)
class ValueObjectArchitectureTest {

    private static final DescribedPredicate<JavaClass> UUID_VALUE = describe("have a UUID value", ArchitectureSupport::hasUuidValue);

    @ArchTest
    static final ArchRule classesInSharedIdAreValueObjects = classes() //
            .that().resideInAPackage("..shared.id..") //
            .and().areTopLevelClasses() //
            .and().areNotInterfaces() //
            .and().doNotBelongToAnyOf(ValueObjectAssert.class) //
            .should().implement(ValueObject.class);

    @ArchTest
    static final ArchRule idsCanBeCreatedAtRandom = classes() //
            .that().implement(ValueObject.class) //
            .and(UUID_VALUE) //
            .should(classCondition("have a static random()", ArchitectureSupport::hasRandom));

    @ArchTest
    static final ArchRule valuesFromDevicesAreNeverInvented = classes() //
            .that().implement(ValueObject.class) //
            .and(not(UUID_VALUE)) //
            .should(classCondition("have no static random()", javaClass -> !hasRandom(javaClass))) //
            .because("a MAC address, an RFID tag id or an IP address comes from a device – tests take theirs from RandomValues");

}
