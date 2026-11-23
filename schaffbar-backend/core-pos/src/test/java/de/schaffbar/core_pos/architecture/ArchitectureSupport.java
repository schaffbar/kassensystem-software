package de.schaffbar.core_pos.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.and;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.schaffbar.core_pos.shared.cqrs.Command;
import de.schaffbar.core_pos.shared.cqrs.Query;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.id.ValueObject;

final class ArchitectureSupport {

    static final String ROOT = "de.schaffbar.core_pos";

    static final String DOMAIN = "..domain..";

    static final String WEB = "..domain..web..";

    // controllers that belong to no aggregate (web.device, web.dashboard) – spelled out, "..web.." would match shared.web
    static final String WEB_OUTSIDE_AGGREGATES = ROOT + ".web..";

    static final String USE_CASE = "..use_case..";

    // the aggregate root is named after its package: rfid_tag_assignment → RfidTagAssignment
    static final DescribedPredicate<JavaClass> AGGREGATE = and( //
            annotatedWith(Entity.class).forSubtype(), //
            resideInAPackage(DOMAIN), //
            DescribedPredicate.describe("named after its package", ArchitectureSupport::isNamedAfterItsPackage)) //
            .as("aggregates");

    static final DescribedPredicate<JavaClass> INNER_ENTITY = and( //
            annotatedWith(Entity.class).forSubtype(), //
            resideInAPackage(DOMAIN), //
            not(AGGREGATE)) //
            .as("inner entities");

    static final DescribedPredicate<JavaClass> DOMAIN_SERVICE = and( //
            annotatedWith(Service.class).forSubtype(), //
            resideInAPackage(DOMAIN)) //
            .as("domain services");

    static final String READ_FACADE = "..read_facade..";

    // also the older @Transactional without @Command (jakarta's or Spring's without readOnly) until the rollout
    static final DescribedPredicate<JavaMethod> WRITE_TRANSACTION = DescribedPredicate.describe("a @Command (writing transaction)", //
            method -> method.isAnnotatedWith(Command.class) || method.isAnnotatedWith(jakarta.transaction.Transactional.class) || method.isAnnotatedWith(
                    Transactional.class) && !isReadOnlyTransaction(method));

    static final DescribedPredicate<JavaMethod> RETURNS_EVENTS = DescribedPredicate.describe("return List<SchaffbarEvent>",
            method -> isListOf(method.getReturnType(), SchaffbarEvent.class));

    static final DescribedPredicate<JavaMethodCall> REPOSITORY_METHOD_TAKING_UUID = DescribedPredicate.describe("a repository method taking a UUID", //
            call -> call.getTargetOwner().getSimpleName().endsWith("Repository") && call.getTarget().getRawParameterTypes().stream().anyMatch(
                    ArchitectureSupport::isUuid));

    // the inherited Spring Data methods that take the raw id – its type parameter is erased to Object, so
    // REPOSITORY_METHOD_TAKING_UUID does not see them; a value object overload of the repository is fine
    static final DescribedPredicate<JavaMethodCall> REPOSITORY_METHOD_BY_RAW_ID = DescribedPredicate.describe(
            "deleteById, deleteAllById, deleteAllByIdInBatch or existsById with the raw id", //
            call -> call.getTargetOwner().getSimpleName().endsWith("Repository") //
                    && Set.of("deleteById", "deleteAllById", "deleteAllByIdInBatch", "existsById").contains(call.getName()) //
                    && call.getTarget().getRawParameterTypes().stream().noneMatch(ArchitectureSupport::isValueObject));

    private ArchitectureSupport() {
    }

    // ------------------------------------------------------------------------
    // predicates

    static boolean isNamedAfterItsPackage(JavaClass javaClass) {
        String packageName = javaClass.getPackageName().substring(javaClass.getPackageName().lastIndexOf('.') + 1);
        String aggregateName = Stream.of(packageName.split("_")) //
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1)) //
                .reduce("", String::concat);
        return javaClass.getSimpleName().equals(aggregateName);
    }

    static boolean isValueObject(JavaClass javaClass) {
        return javaClass.isAssignableTo(ValueObject.class);
    }

    static boolean hasUuidValue(JavaClass javaClass) {
        return javaClass.tryGetField("value").map(field -> isUuid(field.getRawType())).orElse(false);
    }

    static boolean hasRandom(JavaClass javaClass) {
        return javaClass.getMethods().stream().anyMatch(method -> method.getName().equals("random") && isStatic(method) && method.getRawParameterTypes().isEmpty());
    }

    static boolean isUuid(JavaClass javaClass) {
        return javaClass.isEquivalentTo(UUID.class);
    }

    static boolean isReadOnlyTransaction(JavaMethod method) {
        if (method.isAnnotatedWith(Query.class)) {
            return true;
        }
        return method.tryGetAnnotationOfType(Transactional.class.getName()) //
                .flatMap(transactional -> transactional.get("readOnly")) //
                .map(Boolean.TRUE::equals) //
                .orElse(false);
    }

    static boolean isStatic(JavaMethod method) {
        return method.getModifiers().contains(JavaModifier.STATIC);
    }

    // Lombok's @Builder is gone after compiling – the generated builder() is what remains
    static boolean hasBuilder(JavaClass javaClass) {
        return javaClass.getMethods().stream().anyMatch(method -> method.getName().equals("builder") && isStatic(method));
    }

    static boolean isNestedIn(JavaClass javaClass, String enclosingSuffix) {
        return javaClass.getEnclosingClass().map(enclosing -> enclosing.getSimpleName().endsWith(enclosingSuffix)).orElse(false);
    }

    static boolean isListOf(JavaType type, Class<?> elementType) {
        return type instanceof JavaParameterizedType parameterized //
                && parameterized.toErasure().isEquivalentTo(List.class) && parameterized.getActualTypeArguments().getFirst().toErasure().isEquivalentTo(
                elementType);
    }

    // the type itself or one of its type arguments (Optional<X>, List<X>, Map<K, V>)
    static boolean mentions(JavaType type, DescribedPredicate<JavaClass> predicate) {
        if (predicate.test(type.toErasure())) {
            return true;
        }
        return type instanceof JavaParameterizedType parameterized && parameterized.getActualTypeArguments().stream().anyMatch(
                argument -> mentions(argument, predicate));
    }

    // ------------------------------------------------------------------------
    // conditions

    static ArchCondition<JavaClass> classCondition(String description, Predicate<JavaClass> predicate) {
        return new ArchCondition<>(description) {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                if (!predicate.test(javaClass)) {
                    events.add(SimpleConditionEvent.violated(javaClass, javaClass.getName()));
                }
            }
        };
    }

    static ArchCondition<JavaMethod> methodCondition(String description, Predicate<JavaMethod> predicate) {
        return new ArchCondition<>(description) {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                if (!predicate.test(method)) {
                    events.add(SimpleConditionEvent.violated(method, method.getFullName()));
                }
            }
        };
    }

}
