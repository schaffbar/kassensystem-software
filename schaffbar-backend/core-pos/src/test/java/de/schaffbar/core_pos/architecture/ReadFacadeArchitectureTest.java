package de.schaffbar.core_pos.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.READ_FACADE;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.ROOT;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.USE_CASE;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.WRITE_TRANSACTION;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.shared.cqrs.Query;

@AnalyzeClasses(packages = ROOT, importOptions = ImportOption.DoNotIncludeTests.class)
class ReadFacadeArchitectureTest {

    @ArchTest
    static final ArchRule readFacadePackageContainsReadFacadesAndViews = classes() //
            .that().resideInAPackage(READ_FACADE) //
            .and().areTopLevelClasses() //
            .should().haveSimpleNameEndingWith("ReadFacade") //
            .orShould().haveSimpleNameEndingWith("Views");

    @ArchTest
    static final ArchRule readFacadesAreValidatedServices = classes() //
            .that().resideInAPackage(READ_FACADE) //
            .and().haveSimpleNameEndingWith("ReadFacade") //
            .should().bePublic() //
            .andShould().beAnnotatedWith(Service.class) //
            .andShould().beAnnotatedWith(Validated.class);

    @ArchTest
    static final ArchRule readFacadesOnlyRead = methods() //
            .that().areDeclaredInClassesThat().resideInAPackage(READ_FACADE) //
            .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("ReadFacade") //
            .and().arePublic() //
            .should().beAnnotatedWith(Query.class) //
            .because("a read facade answers the queries of one controller – changes go through use cases (E17)");

    @ArchTest
    static final ArchRule readFacadesCallNoCommands = noClasses() //
            .that().resideInAPackage(READ_FACADE) //
            .should().callMethodWhere(DescribedPredicate.describe("a @Command", //
                    call -> call.getTarget().resolveMember().map(WRITE_TRANSACTION::test).orElse(false))) //
            .orShould().dependOnClassesThat().resideInAPackage(USE_CASE);

}
