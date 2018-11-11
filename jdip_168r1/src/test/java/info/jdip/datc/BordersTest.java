package info.jdip.datc;

import info.jdip.misc.TestSuite;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;
import java.util.Locale;

public class BordersTest extends DATCTest {

    @BeforeAll
    public static void setTestCaseLocation(){
        Locale.setDefault(Locale.US);
        testSuite = new TestSuite(new File("build/distributions"));
        testCaseLocation = "etc/test_data/borders.txt";
        testSuite.parseCases(new File(testCaseLocation));


    }
}
