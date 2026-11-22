package de.schaffbar.core_pos.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.and;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.DOMAIN_SERVICE;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.READ_FACADE;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.ROOT;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.USE_CASE;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.methodCondition;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.transaction.annotation.Transactional;

import de.schaffbar.core_pos.shared.cqrs.Command;
import de.schaffbar.core_pos.shared.cqrs.Query;

@AnalyzeClasses(packages = ROOT, importOptions = ImportOption.DoNotIncludeTests.class)
class QueryCommandArchitectureTest {

    private static final DescribedPredicate<JavaClass> SERVICES_USE_CASES_AND_READ_FACADES = DOMAIN_SERVICE //
            .or(resideInAPackage(USE_CASE)) //
            .or(and(resideInAPackage(READ_FACADE), simpleNameEndingWith("ReadFacade"))) //
            .as("services, use cases and read facades");

    @ArchTest
    static final ArchRule publicMethodsAreQueriesOrCommands = methods() //
            .that().areDeclaredInClassesThat(SERVICES_USE_CASES_AND_READ_FACADES) //
            .and().arePublic() //
            .should(methodCondition("be annotated with either @Query or @Command",
                    method -> method.isAnnotatedWith(Query.class) ^ method.isAnnotatedWith(Command.class))) //
            .because("the annotation says whether a method reads or writes and brings its transaction (E21)");

    @ArchTest
    static final ArchRule methodsHaveNoTransactionalOfTheirOwn = methods() //
            .that().areDeclaredInClassesThat(SERVICES_USE_CASES_AND_READ_FACADES) //
            .should().notBeAnnotatedWith(Transactional.class) //
            .andShould().notBeAnnotatedWith(jakarta.transaction.Transactional.class) //
            .because("the transaction comes with @Query or @Command – one place to change it (E21)");

    @ArchTest
    static final ArchRule classesHaveNoTransactionalOfTheirOwn = classes() //
            .that(SERVICES_USE_CASES_AND_READ_FACADES) //
            .should().notBeAnnotatedWith(Transactional.class) //
            .andShould().notBeAnnotatedWith(jakarta.transaction.Transactional.class);

    @ArchTest
    static final ArchRule queriesReturnSomething = methods() //
            .that().areAnnotatedWith(Query.class) //
            .should(methodCondition("return a value", method -> !method.getRawReturnType().getName().equals("void")));

    @ArchTest
    static final ArchRule queriesAndCommandsOnlyInServicesUseCasesAndReadFacades = methods() //
            .that().areAnnotatedWith(Query.class) //
            .or().areAnnotatedWith(Command.class) //
            .should().beDeclaredInClassesThat(SERVICES_USE_CASES_AND_READ_FACADES) //
            .because("a repository query is Spring Data's @Query, not ours");

    @ArchTest
    static final ArchRule springDataQueryOnlyInRepositories = noClasses() //
            .that().haveSimpleNameNotEndingWith("Repository") //
            .should().dependOnClassesThat().haveFullyQualifiedName("org.springframework.data.jpa.repository.Query") //
            .because("outside of repositories @Query is de.schaffbar.core_pos.shared.cqrs.Query");

}
