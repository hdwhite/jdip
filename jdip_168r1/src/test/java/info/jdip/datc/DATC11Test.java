package info.jdip.datc;

import dip.misc.TestSuite;
import java.io.File;
import org.junit.jupiter.api.BeforeAll;

public class DATC11Test extends DATCTest {

    @BeforeAll
    public static void setTestCaseLocation() {
        testSuite = new TestSuite(new File("build/distributions"));
        testCaseLocation = "etc/test_data/datc_v2.4_11.txt";
        testSuite.parseCases(new File(testCaseLocation));
    }
}
