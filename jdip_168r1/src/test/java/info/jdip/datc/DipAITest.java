package info.jdip.datc;

import info.jdip.misc.TestSuite;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;

public class DipAITest extends DATCTest {

    @BeforeAll
    public static void setTestCaseLocation() {
        testSuite = new TestSuite(new File("build/tmp/variants"));
        testCaseLocation = "etc/test_data/dipai.txt";
        testSuite.parseCases(new File(testCaseLocation));
    }
}
