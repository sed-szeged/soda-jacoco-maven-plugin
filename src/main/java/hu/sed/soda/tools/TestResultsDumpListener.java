package hu.sed.soda.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Custom test results execution listener for JUnit 4.x and TestNG.
 */
public class TestResultsDumpListener extends RunListener implements ITestListener {

  private static final Logger LOGGER = Logger.getLogger(TestResultsDumpListener.class.getName());
  
  /**
   * Directory for coverage data.
   */
  private static File outputDirectory;
    
  /**
   * The version number of the program under test.
   * 
   * TODO: Get this value automatically.
   */
  private static String revision = "0";

  /**
   * Numeric index of test for creating unique file names.
   */
  private static long testIndex = 0;

  /**
   * The results of tests with method level granularity.
   */
  private static List<TestInfo> testResults = new LinkedList<TestInfo>();

  /**
   * Information about the test which is running at the moment.
   */
  private static TestInfo actualTestInfo = null;

  /**
   * Statistics about the test suite.
   */
  private static Map<JUnitStatus, Long> testStats = new HashMap<JUnitStatus, Long>();

  /**
   * Initializes the output directory and the log output stream.
   */
  static {
    try {
      outputDirectory = new File(Constants.BASE_DIR);

      if (!outputDirectory.exists()) {
        outputDirectory.mkdirs();
      }
      
      // Configuring the logger.
      FileHandler fileHandler = new FileHandler(new File(Constants.BASE_DIR, "CustomJUnitExecutionListener.log").getAbsolutePath(), true);
      fileHandler.setFormatter(new SimpleFormatter());

      LOGGER.addHandler(fileHandler);

      // Initializing the statistics.
      for (JUnitStatus status : JUnitStatus.values()) {
        testStats.put(status, Long.valueOf(0));
      }

      LOGGER.info("Custom run listener has been initialized successfully.");
    } catch (SecurityException | IOException e) {
      System.err.println(e);
    }
  }

  /**
   * Writes the collected test results, and the hash mapping to files in the output directory.
   * The results will be written into the <{@link Constants#BASE_DIR}>/<{@link #revisionNumber}>/TestResults.r<{@link #revision}> file.
   * The mapping will be written into the <{@link Constants#BASE_DIR}>/<{@link #revisionNumber}>/{@link Constants#MAP_FILE}.r<{@link #revision}> file.
   * 
   * @param testResults
   *          List of {@link TestInfo test information} object.
   * @param revision
   *          The version identifier of the actual program under test.
   */
  public static void dumpTestResults() {
    File resultsDir = new File(Constants.BASE_DIR, revision);
    if (!resultsDir.exists()) {
      resultsDir.mkdirs();
    }

    File resultsFile = new File(resultsDir, String.format("TestResults.r%s", revision));
    try (
        BufferedWriter resultOutput = new BufferedWriter(new FileWriter(resultsFile, true));
    ) {
      for (TestInfo result : testResults) {
        resultOutput.write(String.format("%s: %s\n", result.getFinalStatus().getOutcome(), result.getTestName()));
      }
    } catch (IOException e) {
      LOGGER.warning("Cannot, dump test results because: " + e.getMessage());
    }
  }

  // //////////////////////////////////////////////////////////////////////////
  // JUnit ////////////////////////////////////////////////////////////////////
  // //////////////////////////////////////////////////////////////////////////

  /**
   * Creates a name for the given test and updates the status of that test.
   * 
   * @param description
   *          The {@link Description description} of the test.
   * @param status
   *          The {@link JUnitStatus status} of the test.
   */
  private void handleEvent(Description description, JUnitStatus status) {
    testStats.put(status, testStats.get(status).longValue() + 1);

    actualTestInfo.addStatus(status);

    LOGGER.info(String.format("%s %s", actualTestInfo.getTestName(), status));
  }

  @Override
  public void testRunStarted(Description description) throws Exception {
    LOGGER.info("TEST RUN STARTED");
  }

  @Override
  public void testIgnored(Description description) throws Exception {
    actualTestInfo = new TestInfo(TestInfo.getTestName(description));

    handleEvent(description, JUnitStatus.IGNORED);

    super.testIgnored(description);
  }

  @Override
  public void testStarted(Description description) throws Exception {
    actualTestInfo = new TestInfo(TestInfo.getTestName(description));
    handleEvent(description, JUnitStatus.STARTED);
    super.testStarted(description);
  }

  @Override
  public void testAssumptionFailure(Failure failure) {
    handleEvent(failure.getDescription(), JUnitStatus.ASSUMPTION_FAILED);

    super.testAssumptionFailure(failure);
  }

  @Override
  public void testFailure(Failure failure) throws Exception {
    handleEvent(failure.getDescription(), JUnitStatus.FAILED);

    super.testFailure(failure);
  }

  @Override
  public void testFinished(Description description) throws Exception {
    handleEvent(description, JUnitStatus.FINISHED);
    testResults.add(actualTestInfo);
    super.testFinished(description);
  }

  @Override
  public void testRunFinished(Result result) throws Exception {
    LOGGER.info(String.format("TEST RUN FINISHED in %dms", result.getRunTime()));
    LOGGER.info(String.format("JUnit stats: {tests=%d, ignored=%d, failed=%d}", result.getRunCount(), result.getIgnoreCount(), result.getFailureCount()));
    LOGGER.info(String.format("Listener stats: %s", testStats));

    dumpTestResults();

    super.testRunFinished(result);
  }

  // //////////////////////////////////////////////////////////////////////////
  // TestNG ///////////////////////////////////////////////////////////////////
  // //////////////////////////////////////////////////////////////////////////

  /**
   * Creates a name for the given test and updates the status of that test.
   * 
   * @param result
   *          The {@link ITestResult result} of the test.
   */
  private void handleEvent(ITestResult result) {
    String testName = TestInfo.getTestName(result);
    TestNGStatus status = TestNGStatus.createFrom(result);

    TestInfo info = new TestInfo(testName, status);

    LOGGER.info(String.format("%s %s", testName, status));

    if (status != TestNGStatus.STARTED && status != TestNGStatus.SKIPPED) {
      testResults.add(info);
    }
  }

  @Override
  public void onTestStart(ITestResult result) {
    handleEvent(result);
  }

  @Override
  public void onTestSuccess(ITestResult result) {
    handleEvent(result);
  }

  @Override
  public void onTestFailure(ITestResult result) {
    handleEvent(result);
  }

  @Override
  public void onTestSkipped(ITestResult result) {
    handleEvent(result);
  }

  @Override
  public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
    handleEvent(result);
  }

  @Override
  public void onStart(ITestContext context) {
    LOGGER.info(String.format("TEST (%s) STARTED", context.getName()));
  }

  @Override
  public void onFinish(ITestContext context) {
    LOGGER.info(String.format("TEST (%s) FINISHED in %dms", context.getName(), context.getEndDate().getTime() - context.getStartDate().getTime()));
    LOGGER.info(String.format("TestNG stats: {tests=%d, skipped=%d, succeeded=%d, failed=%d, percent=%d, index=%d}",
        context.getAllTestMethods().length, context.getSkippedTests().size(), context.getPassedTests().size(), context.getFailedTests().size(), context.getFailedButWithinSuccessPercentageTests().size(), testIndex));

    dumpTestResults();
  }
}