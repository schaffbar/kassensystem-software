package de.schaffbar.core_pos.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.ROOT;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.classCondition;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.hasRandom;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.isUuid;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.isValueObject;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.mentions;

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

    private static final DescribedPredicate<JavaClass> VALUE_OBJECT_OR_UUID = describe("a value object or a UUID",
            javaClass -> isValueObject(javaClass) || isUuid(javaClass));

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

    @ArchTest
    static final ArchRule responsesCarryValuesAsStrings = classes() //
            .that().haveSimpleNameEndingWith("ApiDto") //
            .should(classCondition("have no value object or UUID in a field (also not in a Set or List)",
                    response -> response.getFields().stream().noneMatch(field -> mentions(field.getType(), VALUE_OBJECT_OR_UUID)))) //
            .because("an id or a value goes out as String (\"id.value\") – one shape for every response, independent of how a value object "
                    + "serializes itself");

}
