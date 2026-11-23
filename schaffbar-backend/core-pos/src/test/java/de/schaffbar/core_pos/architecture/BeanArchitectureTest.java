package de.schaffbar.core_pos.architecture;

import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.ROOT;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.isUuid;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.methodCondition;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// the rules of "Spring beans" in schaffbar-backend/CLAUDE.md: services, use cases, read facades and controllers
@AnalyzeClasses(packages = ROOT, importOptions = ImportOption.DoNotIncludeTests.class)
class BeanArchitectureTest {

    private static final DescribedPredicate<JavaClass> BEANS = DescribedPredicate.<JavaClass>or( //
            annotatedWith(Service.class).forSubtype(), //
            annotatedWith(RestController.class).forSubtype()) //
            .as("services, use cases, read facades and controllers");

    @ArchTest
    static final ArchRule beansArePublicAndValidated = classes() //
            .that(BEANS) //
            .should().bePublic() //
            .andShould().beAnnotatedWith(Validated.class) //
            .because("@Validated makes Spring check the constraints on the parameters");

    @ArchTest
    static final ArchRule beanFieldsArePrivateFinal = fields() //
            .that().areDeclaredInClassesThat(BEANS) //
            .and().areNotStatic() //
            .should().bePrivate() //
            .andShould().beFinal() //
            .because("dependencies come through the constructor (@RequiredArgsConstructor)");

    @ArchTest
    static final ArchRule beansTakeCommandsOrValueObjects = methods() //
            .that().areDeclaredInClassesThat(BEANS) //
            .and().arePublic() //
            .should(methodCondition("take no UUID and no String", method -> method.getRawParameterTypes().stream() //
                    .noneMatch(type -> isUuid(type) || type.isEquivalentTo(String.class)))) //
            .because("ids and values are value objects in every signature (CustomerId, MacAddress, …)");

    @ArchTest
    static final ArchRule beanParametersAreValidated = methods() //
            .that().areDeclaredInClassesThat(BEANS) //
            .and().arePublic() //
            .should(methodCondition("annotate every object parameter with @NotNull, @NotEmpty or @NotBlank", //
                    method -> method.getParameters().stream() //
                            .filter(parameter -> !parameter.getRawType().isPrimitive()) //
                            // an optional filter is null when it is not given (@RequestParam(required = false))
                            .filter(parameter -> !isOptionalRequestParam(parameter)) //
                            .allMatch(parameter -> parameter.isAnnotatedWith(NotNull.class) //
                                    || parameter.isAnnotatedWith(NotEmpty.class) //
                                    || parameter.isAnnotatedWith(NotBlank.class))));

    @ArchTest
    static final ArchRule pathVariablesAndRequestParamsAreValueObjects = methods() //
            .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class) //
            .should(methodCondition("take @PathVariable and @RequestParam as value objects, never UUID or String", //
                    method -> method.getParameters().stream() //
                            .filter(parameter -> parameter.isAnnotatedWith(PathVariable.class) || parameter.isAnnotatedWith(RequestParam.class)) //
                            .noneMatch(parameter -> isUuid(parameter.getRawType()) || parameter.getRawType().isEquivalentTo(String.class)))) //
            .because("each value object has its StringTo…Converter in shared/util/ – nothing is converted by hand");

    // ------------------------------------------------------------------------
    // helper

    private static boolean isOptionalRequestParam(JavaParameter parameter) {
        return parameter.tryGetAnnotationOfType(RequestParam.class.getName()) //
                .flatMap(requestParam -> requestParam.get("required")) //
                .map(Boolean.FALSE::equals) //
                .orElse(false);
    }

}
