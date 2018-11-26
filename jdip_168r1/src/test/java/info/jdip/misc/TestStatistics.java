package info.jdip.misc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class TestStatistics {
    private static final Logger logger = LoggerFactory.getLogger(TestStatistics.class);
    private final List<String> failedCaseNames = new ArrayList<>();
    private List<String> unresolvedCases = new ArrayList<>();
    private int nOrders = 0;
    private int nPass = 0;
    private int nCases = 0;
    private long startMillis;
    private int benchTimes;
    private long endTime;
    private long parsingFinished;


    TestStatistics(long startMillis, int benchTimes) {

        this.startMillis = startMillis;
        this.benchTimes = benchTimes;
    }

    void addOrders(int orders) {
        nOrders += orders;
    }

    void newCase() {
        nCases++;
    }

    void pass() {
        nPass++;
    }

    void fail(String name) {
        failedCaseNames.add(name);
    }

    void endTime(long endTime) {

        this.endTime = endTime;
    }

    private double averageOrderTime() {
        return (double) testingTime() / (double) nOrders;
    }

    private long testingTime() {
        return endTime - parsingFinished;
    }

    private double orderThroughput() {
        return 1000.0d / averageOrderTime();
    }

    private double score() {
        return (double) nPass / (double) nCases * 100.0d;
    }

    private int cases() {
        return nCases;
    }

    private int passed() {
        return nPass;
    }

    int failed() {
        return failedCaseNames.size();
    }

    private int orders() {
        return nOrders;
    }


    void addUnresolved(String caseName) {
        unresolvedCases.add(caseName);
    }


    private double parseTime() {
        return (parsingFinished - startMillis) / 1000d;

    }

    void parsingFinished(long parsingFinished) {

        this.parsingFinished = parsingFinished;
    }

    void printStatistics(boolean isPerformanceTesting) {
        logger.info("End: {}", new Date());

        logger.info("Failed Cases: {}", failedCaseNames.size());
        for (String failedCaseName : failedCaseNames) {
            logger.info("Failed case: {}", failedCaseName);
        }

        logger.info("Unresolved Paradoxes: {}", unresolvedCases.size());
        for (String unRezParadox : unresolvedCases) {
            logger.info("Unresolved paradox: {}", unRezParadox);
        }
        // print to log
        logger.info("Statistics:");
        logger.info("Case parse time: {} seconds.", parseTime());
        if (isPerformanceTesting) {
            logger.info("{} cases evaluated. Pass/Fail rate not available with -perftest option.", cases());
            logger.info("Adjudication Performance for {} iterations:", benchTimes);
        } else {
            logger.info("{} cases evaluated. {} passed, {} failed; {}%  pass rate.", cases(), passed(), failed(), score());
            logger.info("    Times [includes setup, adjudication, and post-adjudication comparision]");
        }
        logger.info("{} orders processed in {} ms; {} ms/order average", orders(), testingTime(), averageOrderTime());
        logger.info("Throughput: {} orders/second", orderThroughput());


    }
}
