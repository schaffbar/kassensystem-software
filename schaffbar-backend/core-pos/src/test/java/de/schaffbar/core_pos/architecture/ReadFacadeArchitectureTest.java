package de.schaffbar.core_pos.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.READ_FACADE;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.ROOT;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.USE_CASE;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.WRITE_TRANSACTION;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.classCondition;

import java.util.Set;
import java.util.stream.Collectors;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

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
            .because("a read facade answers the queries of one controller – changes go through use cases");

    @ArchTest
    static final ArchRule readFacadesCallNoCommands = noClasses() //
            .that().resideInAPackage(READ_FACADE) //
            .should().callMethodWhere(DescribedPredicate.describe("a @Command", //
                    call -> call.getTarget().resolveMember().map(WRITE_TRANSACTION::test).orElse(false))) //
            .orShould().dependOnClassesThat().resideInAPackage(USE_CASE);

    @ArchTest
    static final ArchRule controllersUseTheReadFacadeNamedAfterThem = classes() //
            .that().areAnnotatedWith(RestController.class) //
            .should(classCondition("use exactly one read facade, named after the controller (XController → XReadFacade)", //
                    controller -> readFacadesUsedBy(controller).equals(Set.of(readFacadeNameOf(controller))))) //
            .because("one read facade per controller, named after it (CLAUDE.md, \"Read facades\")");

    @ArchTest
    static final ArchRule readFacadesServeOnlyTheirController = classes() //
            .that().resideInAPackage(READ_FACADE) //
            .and().haveSimpleNameEndingWith("ReadFacade") //
            .should(classCondition("be used only by the controller it is named after (XReadFacade → XController)", //
                    readFacade -> usersOf(readFacade).equals(Set.of(controllerNameOf(readFacade)))));

    // ------------------------------------------------------------------------
    // helper

    private static Set<String> readFacadesUsedBy(JavaClass controller) {
        return controller.getDirectDependenciesFromSelf().stream() //
                .map(Dependency::getTargetClass) //
                .filter(target -> target.getPackageName().endsWith(".read_facade") && target.getSimpleName().endsWith("ReadFacade")) //
                .map(JavaClass::getSimpleName) //
                .collect(Collectors.toSet());
    }

    private static Set<String> usersOf(JavaClass readFacade) {
        return readFacade.getDirectDependenciesToSelf().stream() //
                .map(Dependency::getOriginClass) //
                .filter(origin -> !origin.getName().startsWith(readFacade.getName())) //
                .map(JavaClass::getSimpleName) //
                .collect(Collectors.toSet());
    }

    private static String readFacadeNameOf(JavaClass controller) {
        return controller.getSimpleName().replaceFirst("Controller$", "ReadFacade");
    }

    private static String controllerNameOf(JavaClass readFacade) {
        return readFacade.getSimpleName().replaceFirst("ReadFacade$", "Controller");
    }

}
