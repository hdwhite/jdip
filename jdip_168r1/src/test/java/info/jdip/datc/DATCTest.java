package info.jdip.datc;

import info.jdip.misc.Case;
import info.jdip.misc.TestCaseRunner;
import info.jdip.misc.TestSuite;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

public abstract class DATCTest {

    private static final Logger logger = LoggerFactory.getLogger(DATCTest.class);

    protected static TestSuite testSuite;
    protected static String testCaseLocation;
    final List<String> unRezParadoxes = new LinkedList<>();


    private static List<Case> sourceOfCases() {
        return testSuite.getAllCases();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @DisplayName("DATC parametrized cases")
    @MethodSource("sourceOfCases")
    void withValueSource(Case testCase) {
        assertTimeoutPreemptively(ofSeconds(2), () -> TestCaseRunner.runCase(testCase));

    }
}
