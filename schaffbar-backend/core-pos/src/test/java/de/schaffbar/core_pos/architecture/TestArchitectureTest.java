package de.schaffbar.core_pos.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static de.schaffbar.core_pos.architecture.ArchitectureSupport.ROOT;

import java.util.Set;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

// rules for the test code itself – skill backend-testing
@AnalyzeClasses(packages = ROOT, importOptions = ImportOption.OnlyIncludeTests.class)
class TestArchitectureTest {

    private static final Set<String> STUBBING_METHODS = Set.of( //
            "when", "doThrow", "doReturn", "doAnswer", "doNothing", "doCallRealMethod", //
            "given", "willThrow", "willReturn", "willAnswer", "willDoNothing", "willCallRealMethod");

    @ArchTest
    static final ArchRule testsStubInGivenMethods = noClasses() //
            .should().callMethodWhere(DescribedPredicate.describe("a Mockito stub (when, doThrow, …) directly in a test method",
                    TestArchitectureTest::isStubInATestMethod)) //
            .because("a stub sets up a state and is a given… method named after it – the test only reads the steps");

    // ------------------------------------------------------------------------
    // helper

    private static boolean isStubInATestMethod(JavaCall<?> call) {
        boolean inTestMethod = call.getOrigin().isAnnotatedWith(Test.class) || call.getOrigin().isAnnotatedWith(ParameterizedTest.class);
        boolean mockitoStub = (call.getTargetOwner().isEquivalentTo(Mockito.class) || call.getTargetOwner().isEquivalentTo(BDDMockito.class))
                && STUBBING_METHODS.contains(call.getName());

        return inTestMethod && mockitoStub;
    }

}
