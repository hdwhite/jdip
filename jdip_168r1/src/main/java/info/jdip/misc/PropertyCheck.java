package info.jdip.misc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Given 2 or more .properties files, it compares them, and writes
 * (to stdout) which property file is missing a key (compared to all
 * other files).
 */
public class PropertyCheck {
    private static final Logger logger = LoggerFactory.getLogger(PropertyCheck.class);
    final Properties[] props;
    final String[] names;

    public PropertyCheck(String[] args)
            throws IOException {
        this.names = args;
        this.props = new Properties[names.length];
        for (int i = 0; i < props.length; i++) {
            props[i] = new Properties();
            props[i].load(new BufferedInputStream(new FileInputStream(names[i])));
        }
    }// PropertyCheck()

    public static void main(String args[])
            throws IOException {
        if (args.length < 2) {
            logger.error("Only {} properties have been specified. Two or more properties files must be specified.", args.length);
            System.exit(1);
        }

        PropertyCheck pc = new PropertyCheck(args);
        pc.check();
    }// main()

    public void check() {
        for (int i = 0; i < props.length; i++) {
            Properties p = props[i];

            logger.info("CHECKING: {}", names[i]);
            logger.info("Missing Keys:");
            boolean noneMissing = true;

            for (int j = 0; j < props.length; j++) {
                if (i != j) {
                    // go through all entries in 'j' and compare to 'i'.
                    // if 'i' doesn't contain an entry, print that, and
                    // print which file it was from.
                    //
                    final String name = names[j];
                    Enumeration e = props[j].propertyNames();
                    while (e.hasMoreElements()) {
                        final String key = (String) e.nextElement();
                        if (p.getProperty(key) == null) {
                            logger.info("Missing {} (from {}) ", key, name);
                            noneMissing = false;
                        }
                    }
                }
            }

            if (noneMissing) {
                logger.info("no keys missing.");
            }
        }
    }// check()


}// class PropertyCheck
