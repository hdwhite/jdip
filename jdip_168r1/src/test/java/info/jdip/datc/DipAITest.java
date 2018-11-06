package info.jdip.datc;

import dip.misc.TestSuite;
import java.io.File;
import org.junit.jupiter.api.BeforeAll;

public class DipAITest extends DATCTest {

    @BeforeAll
    public static void setTestCaseLocation() {
        testSuite = new TestSuite(new File("build/distributions"));
        testCaseLocation = "etc/test_data/dipai.txt";
        testSuite.parseCases(new File(testCaseLocation));
    }
}
