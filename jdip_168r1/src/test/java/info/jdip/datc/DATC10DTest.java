package info.jdip.datc;

import info.jdip.misc.TestSuite;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;

public class DATC10DTest extends DATCTest {

    @BeforeAll
    public static void setTestCaseLocation() {
        testSuite = new TestSuite(new File("build/distributions"));
        testCaseLocation = "etc/test_data/datc_v2.4_10D.txt";
        testSuite.parseCases(new File(testCaseLocation));
    }
}
