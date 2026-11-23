package de.schaffbar.core_pos.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.and;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.metaAnnotatedWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.DOMAIN;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.DOMAIN_SERVICE;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.READ_FACADE;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.ROOT;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.USE_CASE;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.WEB;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.WEB_OUTSIDE_AGGREGATES;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import de.schaffbar.core_pos.shared.event.outbox.OutboxEventRepository;
import de.schaffbar.core_pos.shared.event.outbox.OutboxEventStore;

@AnalyzeClasses(packages = ROOT, importOptions = ImportOption.DoNotIncludeTests.class)
class DependencyArchitectureTest {

    // ------------------------------------------------------------------------
    // aggregates

    @ArchTest
    static final ArchRule servicesDoNotCallEachOther = noClasses() //
            .that(DOMAIN_SERVICE) //
            .should().dependOnClassesThat(DOMAIN_SERVICE) //
            .because("an aggregate changes only itself – everything with a second aggregate is a use case");

    @ArchTest
    static final ArchRule domainDoesNotUseUseCases = noClasses() //
            .that().resideInAPackage(DOMAIN) //
            .and().resideOutsideOfPackage(WEB) //
            .should().dependOnClassesThat().resideInAnyPackage(USE_CASE, READ_FACADE) //
            .because("use cases and read facades use the aggregates, not the other way round");

    @ArchTest
    static final ArchRule domainDoesNotKnowTheApi = noClasses() //
            .that().resideInAPackage(DOMAIN) //
            .and().resideOutsideOfPackage(WEB) //
            .should().dependOnClassesThat().resideInAnyPackage(WEB, WEB_OUTSIDE_AGGREGATES) //
            .because("the API models are a contract with the portal, the domain must not depend on it");

    // ------------------------------------------------------------------------
    // controllers

    @ArchTest
    static final ArchRule controllersLiveInAWebPackage = classes() //
            .that().areAnnotatedWith(RestController.class) //
            .should().resideInAnyPackage(WEB, WEB_OUTSIDE_AGGREGATES) //
            .because("a controller belongs to its aggregate (domain.<aggregate>.web) or to an object without one (web.<object>)");

    @ArchTest
    static final ArchRule controllersKnowOnlyUseCasesAndReadFacades = noClasses() //
            .that().areAnnotatedWith(RestController.class) //
            .should().dependOnClassesThat(and( //
                    metaAnnotatedWith(Component.class).forSubtype(), //
                    // @RestController itself is meta-annotated with @Component
                    DescribedPredicate.<JavaClass>describe("no annotation", javaClass -> !javaClass.isAnnotation()), //
                    not(resideInAnyPackage(USE_CASE, READ_FACADE))).as("are beans other than use cases and read facades")) //
            .because("changes go through use cases, queries through the controller's read facade (E2, E17)");

    // ------------------------------------------------------------------------
    // events

    @ArchTest
    static final ArchRule onlyTheOutboxEventStoreSavesOutboxEvents = noClasses() //
            .that().doNotHaveFullyQualifiedName(OutboxEventStore.class.getName()) //
            .should().dependOnClassesThat().areAssignableTo(OutboxEventRepository.class) //
            .because("events are saved in the transaction of the aggregate change (OutboxEventStore)");

    // ------------------------------------------------------------------------
    // shared

    @ArchTest
    static final ArchRule sharedDoesNotDependOnFeatures = noClasses() //
            .that().resideInAPackage("..shared..") //
            // the event payloads carry the enums and value objects of their aggregate
            .and().resideOutsideOfPackage("..shared.event.payload..") //
            .should().dependOnClassesThat().resideInAnyPackage(DOMAIN, USE_CASE, READ_FACADE, WEB_OUTSIDE_AGGREGATES) //
            .because("shared is used by everyone and knows no feature");

}
