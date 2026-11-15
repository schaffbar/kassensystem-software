package de.schaffbar.core_pos.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.ROOT;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.USE_CASE;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.aggregateNames;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.classCondition;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.shared.cqrs.Command;

@AnalyzeClasses(packages = ROOT, importOptions = ImportOption.DoNotIncludeTests.class)
class UseCaseArchitectureTest {

    @ArchTest
    static final ArchRule useCasesAreValidatedServices = classes() //
            .that().resideInAPackage(USE_CASE) //
            .and().areTopLevelClasses() //
            .should().bePublic() //
            .andShould().beAnnotatedWith(Service.class) //
            .andShould().beAnnotatedWith(Validated.class);

    @ArchTest
    static final ArchRule useCasesHaveOnlyProcess = methods() //
            .that().areDeclaredInClassesThat().resideInAPackage(USE_CASE) //
            .and().arePublic() //
            .should().haveName("process") //
            .because("a use case is one action");

    @ArchTest
    static final ArchRule useCasesAreCommands = freeze(methods() //
            .that().areDeclaredInClassesThat().resideInAPackage(USE_CASE) //
            .and().haveName("process") //
            .should().beAnnotatedWith(Command.class) //
            .because("the use case is the transaction of the change (E21 in docs/backend-consistency.md)"));

    @ArchTest
    static final ArchRule useCasesAreNamedAfterTheirAggregate = freeze(classes() //
            .that().resideInAPackage(USE_CASE) //
            .and().areTopLevelClasses() //
            .should(classCondition("start with the name of an aggregate (<Aggregate><Action>)",
                    useCase -> aggregateNames(useCase).anyMatch(useCase.getSimpleName()::startsWith))) //
            .because("E3 in docs/backend-consistency.md"));

}
