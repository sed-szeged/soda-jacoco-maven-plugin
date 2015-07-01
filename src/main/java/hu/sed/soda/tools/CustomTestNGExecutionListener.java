package hu.sed.soda.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Custom test execution listener for TestNG.
 * 
 * TODO: Rewrite, remove clones.
 */
public class CustomTestNGExecutionListener implements ITestListener {

  private static final Logger LOGGER = Logger.getLogger(CustomTestNGExecutionListener.class.getName());

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
   * Numeric index of test for creating unique file names.
   */
  private static long testIndex = 0;

  /**
   * The results of tests with method level granularity.
   */
  private static Map<String, TestNGStatus> testResults = new HashMap<String, TestNGStatus>();

  /**
   * Initializes the output directory and the log output stream.
   */
  static {
    try {
      outputDirectory = Constants.COVERAGE_DIR.toFile();

      if (!outputDirectory.exists()) {
        outputDirectory.mkdirs();
      }

      // Configuring the logger.
      FileHandler fileHandler = new FileHandler(new File(Constants.BASE_DIR, "CustomTestNGExecutionListener.log").getAbsolutePath());
      fileHandler.setFormatter(new SimpleFormatter());

      LOGGER.addHandler(fileHandler);

      LOGGER.info("Custom run listener has been initialized successfully.");
    } catch (SecurityException | IOException e) {
      System.err.println(e);
    }
  }

  /**
   * Creates a name for the given test and updates the status of that test.
   * 
   * @param result
   *          The {@link ITestResult result} of the test.
   */
  private static synchronized void handleEvent(ITestResult result) {
    String testName = Utils.getTestName(result);
    TestNGStatus status = TestNGStatus.createFrom(result);

    LOGGER.info(String.format("%s %s", testName, status));

    if (status != TestNGStatus.STARTED) {
      String fullTestName = String.format("%s.%s", testName, testIndex++);

      File coverageFile = new File(outputDirectory, fullTestName + '.' + Constants.COVERAGE_FILE_EXT);
      Utils.dumpAndResetCoverage(coverageFile);

      testResults.put(fullTestName, status);
    }
  }

  /**
   * Writes the collected test results to a file in the output directory.
   * The results will be written into the <{@link Constants#BASE_DIR}>/<{@link #revisionNumber}>/TestResults.r<{@link #revisionNumber}> file.
   * 
   * @throws IOException
   */
  private static void dumpTestResults() throws IOException {
    File resultsDir = new File(Constants.BASE_DIR, String.valueOf(revisionNumber));

    if (!resultsDir.exists()) {
      resultsDir.mkdirs();
    }

    File resultsFile = new File(resultsDir, String.format("TestResults.r%d", revisionNumber));

    try (BufferedWriter output = new BufferedWriter(new FileWriter(resultsFile))) {
      for (Entry<String, TestNGStatus> result : testResults.entrySet()) {
        output.write(String.format("%s: %s\n", result.getValue().getOutcome(), result.getKey()));
      }
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

    try {
      dumpTestResults();
    } catch (IOException e) {
      LOGGER.warning("Cannot dump test results because: " + e.getMessage());
    }
  }

}
