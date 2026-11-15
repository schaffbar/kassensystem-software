package de.schaffbar.core_pos.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.ROOT;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.classCondition;

import java.util.Set;

import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;

import de.schaffbar.core_pos.shared.exception.BusinessRuleViolationException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;

@AnalyzeClasses(packages = ROOT, importOptions = ImportOption.DoNotIncludeTests.class)
class ExceptionArchitectureTest {

    @ArchTest
    static final ArchRule noGenericExceptions = freeze(GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS //
            .because("a violated rule has its own exception, a bug is an IllegalStateException (E8)"));

    @ArchTest
    static final ArchRule exceptionsHaveAKind = freeze(classes() //
            .that().resideInAPackage("..shared.exception..") //
            .and().areTopLevelClasses() //
            .and().haveSimpleNameEndingWith("Exception") //
            .should().beAssignableTo(ResourceNotFoundException.class) //
            .orShould().beAssignableTo(BusinessRuleViolationException.class) //
            .because("the kind decides the HTTP status in ApiExceptionHandler (E8)"));

    @ArchTest
    static final ArchRule businessRuleViolationsDeclareTheirCode = classes() //
            .that().areAssignableTo(BusinessRuleViolationException.class) //
            .and().doNotHaveModifier(JavaModifier.ABSTRACT) //
            .and().resideInAPackage("..shared.exception..") //
            .should(classCondition("declare public static final String CODE", exception -> exception.tryGetField("CODE") //
                    .map(field -> field.getModifiers().containsAll(Set.of(JavaModifier.PUBLIC, JavaModifier.STATIC, JavaModifier.FINAL))
                            && field.getRawType().isEquivalentTo(String.class)) //
                    .orElse(false))) //
            .because("the code is part of the API – the portal translates it (shared.errors.<CODE>)");

}
