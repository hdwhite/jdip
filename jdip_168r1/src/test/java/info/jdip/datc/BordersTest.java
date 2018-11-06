package info.jdip.datc;

import dip.misc.TestSuite;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;

public class BordersTest extends DATCTest {

    @BeforeAll
    public static void setTestCaseLocation(){
        testSuite = new TestSuite(new File("build/distributions"));
        testCaseLocation = "etc/test_data/borders.txt";
        testSuite.parseCases(new File(testCaseLocation));


    }
}
