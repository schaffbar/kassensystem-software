package de.schaffbar.core_pos;

import static com.tngtech.archunit.base.DescribedPredicate.and;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.springframework.stereotype.Service;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import de.schaffbar.core_pos.shared.event.outbox.OutboxEventRepository;
import de.schaffbar.core_pos.shared.event.outbox.OutboxEventStore;

@AnalyzeClasses(packages = "de.schaffbar.core_pos", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String DOMAIN = "..domain..";

    private static final String DASHBOARD = "..domain.dashboard..";

    private static final DescribedPredicate<JavaClass> DOMAIN_SERVICE = and( //
            annotatedWith(Service.class).forSubtype(), //
            resideInAPackage(DOMAIN), //
            resideOutsideOfPackage(DASHBOARD));

    // ------------------------------------------------------------------------
    // aggregates

    @ArchTest
    static final ArchRule servicesDoNotCallEachOther = noClasses() //
            .that(DOMAIN_SERVICE) //
            .should().dependOnClassesThat(DOMAIN_SERVICE) //
            .because("an aggregate changes only itself – everything with a second aggregate is a use case");

    @ArchTest
    static final ArchRule repositoriesArePackagePrivate = classes() //
            .that().resideInAPackage(DOMAIN) //
            .and().haveSimpleNameEndingWith("Repository") //
            .should().notBePublic() //
            .because("only the service of the aggregate reads and writes it");

    @ArchTest
    static final ArchRule domainDoesNotUseUseCases = noClasses() //
            .that().resideInAPackage(DOMAIN) //
            .and().resideOutsideOfPackage("..domain..web..") //
            .should().dependOnClassesThat().resideInAPackage("..use_case..") //
            .because("use cases orchestrate the aggregates, not the other way round");

    // ------------------------------------------------------------------------
    // controllers

    @ArchTest
    static final ArchRule controllersChangeStateOnlyThroughUseCases = noClasses() //
            .that().resideInAPackage("..domain..web..") //
            // until the rollout of rfid_tag (K4 in docs/roadmap.md) moves create and delete into use cases
            .and().doNotHaveSimpleName("RfidTagController") //
            .should().callMethodWhere(transactionalServiceMethod()) //
            .because("every change goes through a use case (E2 in docs/backend-consistency.md)");

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
            .should().dependOnClassesThat().resideInAnyPackage(DOMAIN, "..use_case..", "..device..") //
            .because("shared is used by everyone and knows no feature");

    // ------------------------------------------------------------------------
    // helper

    private static DescribedPredicate<JavaMethodCall> transactionalServiceMethod() {
        return DescribedPredicate.describe("a @Transactional method of a domain service", call -> //
                DOMAIN_SERVICE.test(call.getTargetOwner()) //
                        && call.getTarget().resolveMember() //
                                .map(method -> method.isAnnotatedWith(
                                        org.springframework.transaction.annotation.Transactional.class)
                                        || method.isAnnotatedWith(jakarta.transaction.Transactional.class)) //
                                .orElse(false));
    }

}
