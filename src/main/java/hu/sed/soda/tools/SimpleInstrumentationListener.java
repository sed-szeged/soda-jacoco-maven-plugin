package hu.sed.soda.tools;


import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class SimpleInstrumentationListener extends RunListener implements ITestListener {

    private static final Logger LOGGER = Logger.getLogger(CustomTestExecutionListener.class.getName());

    /**
     * Directory for coverage data.
     */
    private static File outputDirectory;

    /**
     * Name of the actual test
     */
    private static String actualTest;

    /**
     * List of tests that executed the instrumented code.
     */
    private static Set<String> coveringTests;

    /**
     * Initializes the output directory and the log output stream.
     */
    static {
        try {
            coveringTests = new HashSet<>();

            outputDirectory = new File(Constants.BASE_DIR);

            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs();
            }

            // Configuring the logger.
            FileHandler fileHandler = new FileHandler(new File(Constants.BASE_DIR, "SimpleInstrumentationListener.log").getAbsolutePath(), true);
            fileHandler.setFormatter(new SimpleFormatter());

            LOGGER.addHandler(fileHandler);

            LOGGER.info("Simple instrumentation listener has been initialized successfully.");
        } catch (SecurityException | IOException e) {
            System.err.println(e);
        }
    }

    /**
     * Manual instrumenter. Adds the actual test to coverage.
     */
    public static void recordCoverage() {
        if (actualTest != null) {
            coveringTests.add(actualTest);
        }
    }

    /**
     * Manual instrumenter. Adds the actual test to coverage with mutation identifier.
     *
     * @param mutationId
     *          Mutation identifier
     */
    public static void recordCoverage(String mutationId) {
        if (actualTest != null) {
            coveringTests.add(actualTest + ":" + mutationId);
        }
    }

    // //////////////////////////////////////////////////////////////////////////
    // JUnit ////////////////////////////////////////////////////////////////////
    // //////////////////////////////////////////////////////////////////////////

    @Override
    public void testStarted(Description description) throws Exception {
        actualTest = TestInfo.getTestName(description);

        super.testStarted(description);
    }

    @Override
    public void testFinished(Description description) throws Exception {
        actualTest = null;

        super.testFinished(description);
    }

    @Override
    public void testIgnored(Description description) throws Exception {
        actualTest = null;

        super.testIgnored(description);
    }

    @Override
    public void testAssumptionFailure(Failure failure) {
        actualTest = null;

        super.testAssumptionFailure(failure);
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        dumpResults();

        super.testRunFinished(result);
    }

    // //////////////////////////////////////////////////////////////////////////
    // TestNG ///////////////////////////////////////////////////////////////////
    // //////////////////////////////////////////////////////////////////////////

    @Override
    public void onTestStart(ITestResult iTestResult) {
        actualTest = TestInfo.getTestName(iTestResult);
    }

    @Override
    public void onTestSuccess(ITestResult iTestResult) {
        actualTest = null;
    }

    @Override
    public void onTestFailure(ITestResult iTestResult) {
        actualTest = null;
    }

    @Override
    public void onTestSkipped(ITestResult iTestResult) {
        actualTest = null;
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult iTestResult) {
        actualTest = null;
    }

    @Override
    public void onStart(ITestContext iTestContext) {

    }

    @Override
    public void onFinish(ITestContext iTestContext) {
        dumpResults();
    }

    private void dumpResults() {
        // Dump data
        try {
            PrintWriter out = new PrintWriter(new File(Constants.BASE_DIR, "TestCoverage.csv").getAbsolutePath());
            for (String test : coveringTests) {
                out.println(test);
            }

            out.close();

            LOGGER.info("Simple instrumentation listener has dumped coverage data succesfully (" + coveringTests.size() + " tests were recorded).");
        } catch (FileNotFoundException e) {
            LOGGER.info("Simple instrumentation listener has failed: Output file not found.");
        }
    }
}