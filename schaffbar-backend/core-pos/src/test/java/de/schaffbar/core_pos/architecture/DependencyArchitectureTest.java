package de.schaffbar.core_pos.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.DOMAIN;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.DOMAIN_SERVICE;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.ROOT;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.TRANSACTIONAL;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.USE_CASE;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.WEB;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import de.schaffbar.core_pos.shared.event.outbox.OutboxEventRepository;
import de.schaffbar.core_pos.shared.event.outbox.OutboxEventStore;

@AnalyzeClasses(packages = ROOT, importOptions = ImportOption.DoNotIncludeTests.class)
class DependencyArchitectureTest {

    @ArchTest
    static final ArchRule servicesDoNotCallEachOther = noClasses() //
            .that(DOMAIN_SERVICE) //
            .should().dependOnClassesThat(DOMAIN_SERVICE) //
            .because("an aggregate changes only itself – everything with a second aggregate is a use case");

    // ------------------------------------------------------------------------
    // aggregates

    @ArchTest
    static final ArchRule domainDoesNotUseUseCases = noClasses() //
            .that().resideInAPackage(DOMAIN) //
            .and().resideOutsideOfPackage(WEB) //
            .should().dependOnClassesThat().resideInAPackage(USE_CASE) //
            .because("use cases orchestrate the aggregates, not the other way round");

    @ArchTest
    static final ArchRule domainDoesNotKnowTheApi = noClasses() //
            .that().resideInAPackage(DOMAIN) //
            .and().resideOutsideOfPackage(WEB) //
            .should().dependOnClassesThat().resideInAPackage(WEB) //
            .because("the API models are a contract with the portal, the domain must not depend on it");

    @ArchTest
    static final ArchRule onlyTheOutboxEventStoreSavesOutboxEvents = noClasses() //
            .that().doNotHaveFullyQualifiedName(OutboxEventStore.class.getName()) //
            .should().dependOnClassesThat().areAssignableTo(OutboxEventRepository.class) //
            .because("events are saved in the transaction of the aggregate change (OutboxEventStore)");

    // ------------------------------------------------------------------------
    // controllers

    @ArchTest
    static final ArchRule sharedDoesNotDependOnFeatures = noClasses() //
            .that().resideInAPackage("..shared..") //
            // the event payloads carry the enums and value objects of their aggregate
            .and().resideOutsideOfPackage("..shared.event.payload..") //
            .should().dependOnClassesThat().resideInAnyPackage(DOMAIN, USE_CASE, "..device..") //
            .because("shared is used by everyone and knows no feature");

    // ------------------------------------------------------------------------
    // events

    private static final DescribedPredicate<JavaMethodCall> TRANSACTIONAL_SERVICE_METHOD = DescribedPredicate.describe(
            "a @Transactional method of a domain service", //
            call -> DOMAIN_SERVICE.test(call.getTargetOwner()) && call.getTarget().resolveMember().map(TRANSACTIONAL::test).orElse(false));

    // ------------------------------------------------------------------------
    // shared

    @ArchTest
    static final ArchRule controllersChangeStateOnlyThroughUseCases = freeze(noClasses() //
            .that().resideInAPackage(WEB) //
            .should().callMethodWhere(TRANSACTIONAL_SERVICE_METHOD) //
            .because("every change goes through a use case (E2 in docs/backend-consistency.md)"));

}
