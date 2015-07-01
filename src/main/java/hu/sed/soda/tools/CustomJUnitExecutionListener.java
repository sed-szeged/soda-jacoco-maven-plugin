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

/**
 * Custom execution listener for JUnit 4.x and later.
 */
public class CustomJUnitExecutionListener extends RunListener {

  private static final Logger LOGGER = Logger.getLogger(CustomJUnitExecutionListener.class.getName());

  /**
   * The version number of the program under test.
   * 
   * @TODO: Get this value automatically.
   */
  private static long revisionNumber = 0;

  /**
   * Directory for coverage data.
   */
  private static File outputDirectory;

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
  private static Map<TestStatus, Long> testStats = new HashMap<TestStatus, Long>();

  /**
   * Initializes the output directory and the log output stream.
   */
  static {
    outputDirectory = Constants.COVERAGE_DIR.toFile();

    try {
      if (!outputDirectory.exists()) {
        outputDirectory.mkdirs();
      }

      // Configuring the logger.
      FileHandler fileHandler = new FileHandler(new File(Constants.BASE_DIR, "CustomJUnitExecutionListener.log").getAbsolutePath());
      fileHandler.setFormatter(new SimpleFormatter());

      LOGGER.addHandler(fileHandler);

      // Initializing the statistics.
      for (TestStatus status : TestStatus.values()) {
        testStats.put(status, Long.valueOf(0));
      }

      LOGGER.info("Custom run listener has been initialized successfully.");
    } catch (SecurityException | IOException e) {
      System.err.println(e);
    }
  }

  /**
   * Creates a name for the given test and updates the status of that test.
   * 
   * @param description
   *          The {@link Description description} of the test.
   * @param status
   *          The {@link TestStatus status} of the test.
   */
  private void handleEvent(Description description, TestStatus status) {
    testStats.put(status, testStats.get(status).longValue() + 1);

    actualTestInfo.addStatus(status);

    LOGGER.info(String.format("%s %s %s", description.getDisplayName(), actualTestInfo.getFullTestName(), status));
  }

  /**
   * Writes the collected test results to a file in the output directory.
   * The results will be written into the <{@link Constants#BASE_DIR}>/<{@link #revisionNumber}>/TestResults.r<{@link #revisionNumber}> file.
   * 
   * @throws IOException
   */
  private void dumpTestResults() throws IOException {
    File resultsDir = new File(Constants.BASE_DIR, String.valueOf(revisionNumber));

    if (!resultsDir.exists()) {
      resultsDir.mkdirs();
    }

    File resultsFile = new File(resultsDir, String.format("TestResults.r%d", revisionNumber));

    try (BufferedWriter output = new BufferedWriter(new FileWriter(resultsFile))) {
      for (TestInfo result : testResults) {
        output.write(String.format("%s: %s\n", result.getFinalStatus().getOutcome(), result.getFullTestName()));
      }
    }
  }

  @Override
  public void testRunStarted(Description description) throws Exception {
    LOGGER.info("TEST RUN STARTED");
  }

  @Override
  public void testIgnored(Description description) throws Exception {
    actualTestInfo = new TestInfo(Utils.getTestName(description));

    handleEvent(description, TestStatus.IGNORED);

    testResults.add(actualTestInfo);

    super.testIgnored(description);
  }

  @Override
  public void testStarted(Description description) throws Exception {
    actualTestInfo = new TestInfo(Utils.getTestName(description));

    handleEvent(description, TestStatus.STARTED);

    super.testStarted(description);
  }

  @Override
  public void testAssumptionFailure(Failure failure) {
    handleEvent(failure.getDescription(), TestStatus.ASSUMPTION_FAILED);

    super.testAssumptionFailure(failure);
  }

  @Override
  public void testFailure(Failure failure) throws Exception {
    handleEvent(failure.getDescription(), TestStatus.FAILED);

    super.testFailure(failure);
  }

  @Override
  public void testFinished(Description description) throws Exception {
    handleEvent(description, TestStatus.FINISHED);

    testResults.add(actualTestInfo);
    File coverageFile = new File(outputDirectory, actualTestInfo.getFullTestName() + '.' + Constants.COVERAGE_FILE_EXT);
    outputDirectory.mkdirs();

    Utils.dumpAndResetCoverage(coverageFile);

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
}