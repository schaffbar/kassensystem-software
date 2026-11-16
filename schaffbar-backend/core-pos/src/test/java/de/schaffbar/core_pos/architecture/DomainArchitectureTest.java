package de.schaffbar.core_pos.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.constructors;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.AGGREGATE;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.DASHBOARD;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.DOMAIN;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.DOMAIN_SERVICE;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.INNER_ENTITY;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.REPOSITORY_METHOD_TAKING_UUID;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.RETURNS_EVENTS;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.ROOT;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.WEB;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.WRITE_TRANSACTION;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.classCondition;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.hasBuilder;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.isNestedIn;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.isStatic;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.isUuid;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.isValueObject;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.mentions;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.methodCondition;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaEnumConstant;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.mapstruct.Mapper;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.event.ValidationSupport;
import de.schaffbar.core_pos.shared.event.outbox.OutboxEventStore;

@AnalyzeClasses(packages = ROOT, importOptions = ImportOption.DoNotIncludeTests.class)
class DomainArchitectureTest {

    private static final List<String> REQUIRED_BUILDING_BLOCKS = List.of( //
            "Service", "Repository", "Views", "EventFactory", "PayloadMapper");

    private static final List<String> BUILDING_BLOCKS = List.of( //
            "Commands", "Service", "Repository", "Views", "EventFactory", "PayloadMapper");

    private static final Set<String> OBJECT_METHODS = Set.of("toString", "equals", "hashCode");

    // ------------------------------------------------------------------------
    // aggregate package

    @ArchTest
    static final ArchRule aggregatesHaveAllBuildingBlocks = freeze(classes() //
            .that(AGGREGATE) //
            .should(haveRequiredBuildingBlocks()));

    @ArchTest
    static final ArchRule aggregatePackagesContainOnlyBuildingBlocks = freeze(classes() //
            .that().resideInAPackage(DOMAIN) //
            .and().resideOutsideOfPackages(WEB, DASHBOARD) //
            .and().areTopLevelClasses() //
            .should(classCondition("be an aggregate, an embeddable, an enum or a building block (*Commands, *Service, …)",
                    DomainArchitectureTest::isAggregatePackageMember)) //
            .because("an agent should not invent a *Helper or *Manager – every kind of class has its place"));

    // ------------------------------------------------------------------------
    // aggregate

    @ArchTest
    static final ArchRule aggregatesAreNotPublic = classes() //
            .that(AGGREGATE) //
            .should().notBePublic() //
            .because("only the own service sees the aggregate – everyone else gets views");

    @ArchTest
    static final ArchRule aggregatesHaveNoPublicConstructor = constructors() //
            .that().areDeclaredInClassesThat(AGGREGATE) //
            .should().notBePublic() //
            .because("an aggregate is created by its static factory of(…)");

    @ArchTest
    static final ArchRule aggregatesHaveAStaticFactory = classes() //
            .that(AGGREGATE) //
            .should(classCondition("have a static factory of(…) returning the aggregate", aggregate -> aggregate.getMethods().stream() //
                    .anyMatch(method -> method.getName().equals("of") && isStatic(method) && method.getRawReturnType().equals(aggregate))));

    @ArchTest
    static final ArchRule aggregatesChangeStateOnlyThroughCommands = methods() //
            .that().areDeclaredInClassesThat(AGGREGATE) //
            .and().arePublic() //
            .and().areNotStatic() //
            .should(methodCondition("be a query (get…, is…, has…) or a command returning List<SchaffbarEvent>",
                    method -> isQuery(method) || OBJECT_METHODS.contains(method.getName()) || RETURNS_EVENTS.test(method)));

    @ArchTest
    static final ArchRule aggregateSettersArePrivate = methods() //
            .that().areDeclaredInClassesThat(AGGREGATE) //
            .and().haveNameMatching("set[A-Z].*") //
            .and(not(RETURNS_EVENTS)) //
            .should().bePrivate() //
            .because("the state changes only through commands, which return their events");

    @ArchTest
    static final ArchRule aggregateIdsAreValueObjects = methods() //
            .that().areDeclaredInClassesThat(AGGREGATE) //
            .and().arePublic() //
            .and().haveNameMatching("get[A-Za-z]*Id") //
            .should(methodCondition("return a value object from shared.id", method -> isValueObject(method.getRawReturnType()))) //
            .because("the aggregate stores the raw id, the outside world sees CustomerId, ToolId, …");

    @ArchTest
    static final ArchRule aggregatesDoNotExposeUuids = methods() //
            .that().areDeclaredInClassesThat(AGGREGATE) //
            .and().arePublic() //
            .should(methodCondition("return something other than UUID", method -> !isUuid(method.getRawReturnType())));

    // ------------------------------------------------------------------------
    // inner entity

    @ArchTest
    static final ArchRule innerEntitiesLiveNextToTheirAggregate = classes() //
            .that(INNER_ENTITY) //
            .should(classCondition("live in the package of an aggregate named after the package", //
                    entity -> entity.getPackage().getClasses().stream().anyMatch(AGGREGATE))) //
            .because("an entity that is no aggregate root belongs to the aggregate of its package");

    @ArchTest
    static final ArchRule innerEntitiesAreNotPublic = classes() //
            .that(INNER_ENTITY) //
            .should().notBePublic() //
            .because("an inner entity belongs to its aggregate – only the aggregate's service sees it");

    @ArchTest
    static final ArchRule innerEntitiesAreCreatedOnlyInTheirPackage = methods() //
            .that().areDeclaredInClassesThat(INNER_ENTITY) //
            .and().areStatic() //
            .should().notBePublic() //
            .because("the aggregate's service creates the inner entity together with the aggregate's change");

    @ArchTest
    static final ArchRule innerEntitySettersArePrivate = methods() //
            .that().areDeclaredInClassesThat(INNER_ENTITY) //
            .and().haveNameMatching("set[A-Z].*") //
            .should().bePrivate();

    @ArchTest
    static final ArchRule innerEntitiesHaveNoBuildingBlocksButARepository = classes() //
            .that(INNER_ENTITY) //
            .should(classCondition("have no *Service, *Views, *Commands, *EventFactory or *PayloadMapper of their own", //
                    entity -> entity.getPackage().getClasses().stream() //
                            .map(JavaClass::getSimpleName) //
                            .noneMatch(name -> BUILDING_BLOCKS.stream() //
                                    .filter(suffix -> !suffix.equals("Repository")) //
                                    .anyMatch(suffix -> name.equals(entity.getSimpleName() + suffix))))) //
            .because("its changes are changes of the aggregate: the aggregate's service, views and events cover them");

    // ------------------------------------------------------------------------
    // commands

    @ArchTest
    static final ArchRule commandsArePublicInterfaces = classes() //
            .that().resideInAPackage(DOMAIN) //
            .and().haveSimpleNameEndingWith("Commands") //
            .should().beInterfaces() //
            .andShould().bePublic();

    @ArchTest
    static final ArchRule commandsAreRecordsInCommands = classes() //
            .that().resideInAPackage(DOMAIN) //
            .and().haveSimpleNameEndingWith("Command") //
            .should(classCondition("be a record nested in *Commands", command -> command.isRecord() && isNestedIn(command, "Commands")));

    @ArchTest
    static final ArchRule commandsHaveABuilder = classes() //
            .that().resideInAPackage(DOMAIN) //
            .and().haveSimpleNameEndingWith("Command") //
            .should(classCondition("have a builder() (Lombok @Builder)", ArchitectureSupport::hasBuilder));

    @ArchTest
    static final ArchRule commandsAreValidated = classes() //
            .that().resideInAPackage(DOMAIN) //
            .and().haveSimpleNameEndingWith("Command") //
            .should().beAnnotatedWith(Validated.class);

    @ArchTest
    static final ArchRule commandsReferToAggregatesByValueObjects = fields() //
            .that().areDeclaredInClassesThat().resideInAPackage(DOMAIN) //
            .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("Command") //
            .should().notHaveRawType(UUID.class);

    // ------------------------------------------------------------------------
    // event factory

    @ArchTest
    static final ArchRule eventFactoriesAreFinalAndNotPublic = classes() //
            .that().resideInAPackage(DOMAIN) //
            .and().haveSimpleNameEndingWith("EventFactory") //
            .should().haveModifier(JavaModifier.FINAL) //
            .andShould().notBePublic();

    @ArchTest
    static final ArchRule eventFactoriesCannotBeInstantiated = constructors() //
            .that().areDeclaredInClassesThat().resideInAPackage(DOMAIN) //
            .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("EventFactory") //
            .should().bePrivate();

    @ArchTest
    static final ArchRule eventsAreCreatedOnlyByEventFactories = noClasses() //
            .that().haveSimpleNameNotEndingWith("EventFactory") //
            .and().doNotBelongToAnyOf(SchaffbarEvent.class) //
            .should().callMethodWhere(DescribedPredicate.describe("a SchaffbarEvent.…Event(…) factory", //
                    call -> call.getTargetOwner().isEquivalentTo(SchaffbarEvent.class) && call.getName().endsWith("Event")));

    @ArchTest
    static final ArchRule eventFactoryMethodsValidateThePayload = methods() //
            .that().areDeclaredInClassesThat().resideInAPackage(DOMAIN) //
            .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("EventFactory") //
            .and().haveRawReturnType(SchaffbarEvent.class) //
            .should(methodCondition("validate the payload and create the event with SchaffbarEvent.…Event(…)",
                    method -> callsPayloadValidate(method) && callsSchaffbarEventFactory(method)));

    // ------------------------------------------------------------------------
    // payload mapper

    @ArchTest
    static final ArchRule payloadMappersArePackagePrivateInterfaces = classes() //
            .that().resideInAPackage(DOMAIN) //
            .and().haveSimpleNameEndingWith("PayloadMapper") //
            .should().beInterfaces() //
            .andShould().notBePublic();

    @ArchTest
    static final ArchRule mappersFailOnUnmappedTargets = classes() //
            .that().resideInAPackage(DOMAIN) //
            .and().resideOutsideOfPackage(WEB) //
            .and().areAnnotatedWith(Mapper.class) //
            .should(classCondition("be a @Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)", DomainArchitectureTest::failsOnUnmappedTargets)) //
            .because("a new field of the aggregate must not get lost in a view or an event");

    // ------------------------------------------------------------------------
    // repository

    @ArchTest
    static final ArchRule repositoriesArePackagePrivateInterfaces = classes() //
            .that().resideInAPackage(DOMAIN) //
            .and().haveSimpleNameEndingWith("Repository") //
            .should().beInterfaces() //
            .andShould().notBePublic() //
            .because("only the service of the aggregate reads and writes it");

    @ArchTest
    static final ArchRule repositoryMethodsTakingUuidsHaveAValueObjectVariant = freeze(classes() //
            .that().resideInAPackage(DOMAIN) //
            .and().haveSimpleNameEndingWith("Repository") //
            .should(haveValueObjectVariants()));

    @ArchTest
    static final ArchRule servicesUseTheValueObjectVariants = freeze(noClasses() //
            .that(DOMAIN_SERVICE) //
            .should().callMethodWhere(REPOSITORY_METHOD_TAKING_UUID) //
            .because("the service works with CustomerId, ToolId, … – the raw UUID stays in the repository"));

    // ------------------------------------------------------------------------
    // service

    @ArchTest
    static final ArchRule servicesArePublicAndValidated = classes() //
            .that(DOMAIN_SERVICE) //
            .should().bePublic() //
            .andShould().beAnnotatedWith(Validated.class);

    @ArchTest
    static final ArchRule serviceFieldsArePrivateFinal = fields() //
            .that().areDeclaredInClassesThat(DOMAIN_SERVICE) //
            .and().areNotStatic() //
            .should().bePrivate() //
            .andShould().beFinal();

    @ArchTest
    static final ArchRule serviceHelpersArePrivate = methods() //
            .that().areDeclaredInClassesThat(DOMAIN_SERVICE) //
            .and().areNotPublic() //
            .should().bePrivate() //
            .because("a service has public queries and commands, everything else is private");

    @ArchTest
    static final ArchRule servicesTakeCommandsOrValueObjects = methods() //
            .that().areDeclaredInClassesThat(DOMAIN_SERVICE) //
            .and().arePublic() //
            .should(methodCondition("take no UUID and no String", method -> method.getRawParameterTypes().stream() //
                    .noneMatch(type -> isUuid(type) || type.isEquivalentTo(String.class))));

    @ArchTest
    static final ArchRule serviceParametersAreValidated = methods() //
            .that().areDeclaredInClassesThat(DOMAIN_SERVICE) //
            .and().arePublic() //
            .should(methodCondition("annotate every object parameter with @NotNull, @NotEmpty or @NotBlank", method -> method.getParameters().stream() //
                    .filter(parameter -> !parameter.getRawType().isPrimitive()) //
                    .allMatch(parameter -> parameter.isAnnotatedWith(NotNull.class) || parameter.isAnnotatedWith(NotEmpty.class) || parameter.isAnnotatedWith(
                            NotBlank.class))));

    @ArchTest
    static final ArchRule serviceQueriesReturnViews = methods() //
            .that().areDeclaredInClassesThat(DOMAIN_SERVICE) //
            .and().arePublic() //
            .should(methodCondition("return no aggregate (also not in Optional or List)", method -> !mentions(method.getReturnType(), AGGREGATE)));

    @ArchTest
    static final ArchRule serviceCommandsReturnNothingOrTheNewId = methods() //
            .that().areDeclaredInClassesThat(DOMAIN_SERVICE) //
            .and().arePublic() //
            .and(WRITE_TRANSACTION) //
            .should(methodCondition("return void or the id of a created aggregate",
                    method -> method.getRawReturnType().getName().equals("void") || isValueObject(method.getRawReturnType())));

    @ArchTest
    static final ArchRule serviceCommandsSaveTheirEvents = freeze(methods() //
            .that().areDeclaredInClassesThat(DOMAIN_SERVICE) //
            .and().arePublic() //
            .and(WRITE_TRANSACTION) //
            .should(methodCondition("save the events with OutboxEventStore.save(…)", method -> method.getMethodCallsFromSelf().stream() //
                    .anyMatch(call -> call.getTargetOwner().isEquivalentTo(OutboxEventStore.class)))));

    // ------------------------------------------------------------------------
    // views

    @ArchTest
    static final ArchRule viewsArePublicInterfaces = classes() //
            .that().resideInAPackage(DOMAIN) //
            .and().haveSimpleNameEndingWith("Views") //
            .should().beInterfaces() //
            .andShould().bePublic();

    @ArchTest
    static final ArchRule viewsAreValidatedRecordsWithABuilder = classes() //
            .that().resideInAPackage(DOMAIN) //
            .and().haveSimpleNameEndingWith("View") //
            .should(classCondition("be a record nested in *Views with a builder() (Lombok @Builder)",
                    view -> view.isRecord() && isNestedIn(view, "Views") && hasBuilder(view))) //
            .andShould().beAnnotatedWith(Validated.class);

    @ArchTest
    static final ArchRule viewsReferToAggregatesByValueObjects = fields() //
            .that().areDeclaredInClassesThat().resideInAPackage(DOMAIN) //
            .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("View") //
            .should().notHaveRawType(UUID.class);

    @ArchTest
    static final ArchRule viewsMapTheAggregateWithMapStruct = classes() //
            .that().resideInAPackage(DOMAIN) //
            .and().resideOutsideOfPackage(DASHBOARD) //
            .and().haveSimpleNameEndingWith("Views") //
            .should().beAnnotatedWith(Mapper.class) //
            .because("the views interface is the MapStruct mapper from the aggregate to its views");

    // ------------------------------------------------------------------------
    // helper

    private static boolean isAggregatePackageMember(JavaClass javaClass) {
        return AGGREGATE.test(javaClass) || INNER_ENTITY.test(javaClass) || javaClass.isAnnotatedWith(Embeddable.class) || javaClass.isEnum() || BUILDING_BLOCKS.stream().anyMatch(
                javaClass.getSimpleName()::endsWith) || isGeneratedByMapStruct(javaClass);
    }

    private static boolean isGeneratedByMapStruct(JavaClass javaClass) {
        return javaClass.getRawInterfaces().stream().anyMatch(implemented -> implemented.isAnnotatedWith(Mapper.class));
    }

    private static boolean isQuery(JavaMethod method) {
        return method.getName().matches("(get|is|has)[A-Z].*") && !RETURNS_EVENTS.test(method);
    }

    private static boolean callsPayloadValidate(JavaMethod method) {
        return method.getMethodCallsFromSelf().stream() //
                .anyMatch(call -> call.getName().equals("validate") && call.getTargetOwner().isAssignableTo(ValidationSupport.class));
    }

    private static boolean callsSchaffbarEventFactory(JavaMethod method) {
        return method.getMethodCallsFromSelf().stream() //
                .anyMatch(call -> call.getTargetOwner().isEquivalentTo(SchaffbarEvent.class) && call.getName().endsWith("Event"));
    }

    private static boolean failsOnUnmappedTargets(JavaClass mapper) {
        return mapper.tryGetAnnotationOfType(Mapper.class.getName()) //
                .flatMap(annotation -> annotation.get("unmappedTargetPolicy")) //
                .map(policy -> policy instanceof JavaEnumConstant constant && constant.name().equals("ERROR")) //
                .orElse(false);
    }

    private static ArchCondition<JavaClass> haveRequiredBuildingBlocks() {
        return new ArchCondition<>("have a *Service, *Repository, *Views, *EventFactory and *PayloadMapper named after it") {
            @Override
            public void check(JavaClass aggregate, ConditionEvents events) {
                REQUIRED_BUILDING_BLOCKS.stream() //
                        .map(suffix -> aggregate.getSimpleName() + suffix) //
                        .filter(name -> aggregate.getPackage().getClasses().stream().noneMatch(c -> c.getSimpleName().equals(name))) //
                        .forEach(name -> events.add(SimpleConditionEvent.violated(aggregate, aggregate.getName() + " has no " + name)));
            }
        };
    }

    private static ArchCondition<JavaClass> haveValueObjectVariants() {
        return new ArchCondition<>("have a default method of the same name taking value objects for every method taking a UUID") {
            @Override
            public void check(JavaClass repository, ConditionEvents events) {
                repository.getMethods().stream() //
                        .filter(method -> method.getRawParameterTypes().stream().anyMatch(ArchitectureSupport::isUuid)) //
                        .filter(method -> repository.getMethods().stream().noneMatch(variant -> isValueObjectVariant(variant, method))) //
                        .forEach(method -> events.add(
                                SimpleConditionEvent.violated(method, method.getFullName() + " has no variant of the same name taking value objects")));
            }
        };
    }

    private static boolean isValueObjectVariant(JavaMethod variant, JavaMethod method) {
        return variant != method //
                && variant.getName().equals(
                method.getName()) && variant.getRawParameterTypes().size() == method.getRawParameterTypes().size() && variant.getRawParameterTypes().stream().noneMatch(
                ArchitectureSupport::isUuid);
    }

}
