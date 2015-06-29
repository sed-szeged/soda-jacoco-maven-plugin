package hu.sed.soda.tools;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.jacoco.core.data.ExecutionDataWriter;
import org.jacoco.core.runtime.RemoteControlReader;
import org.jacoco.core.runtime.RemoteControlWriter;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * Custom execution listener for JUnit 4.x and later.
 */
public class CustomJUnitExecutionListener extends RunListener {

  private static final Logger LOGGER = Logger.getLogger(CustomJUnitExecutionListener.class.getName());

  private static final String ADDRESS = "localhost";

  private static final int PORT = 9999;

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
   * The results of tests in <test name, status> format with method level granularity.
   */
  private static Map<String, TestStatus> testResults = new HashMap<String, TestStatus>();

  /**
   * Initializes the output directory and the log output stream.
   */
  static {
    outputDirectory = Constants.COVERAGE_DIR.toFile();

    try {
      if (!outputDirectory.exists()) {
        outputDirectory.mkdirs();
      }

      FileHandler fileHandler = new FileHandler(new File(Constants.BASE_DIR, "CustomJUnitExecutionListener.log").getAbsolutePath());
      fileHandler.setFormatter(new SimpleFormatter());

      LOGGER.addHandler(fileHandler);

      LOGGER.info("Custom run listener has been inizialized successfully.");
    } catch (SecurityException | IOException e) {
      System.err.println(e);
    }
  }

  /**
   * Updates the current status of the given test.
   * 
   * @param testName
   *          The name of the test.
   * @param status
   *          The new status of the test.
   */
  private void updateTestStatus(final String testName, TestStatus status) {
    TestStatus currentStatus = testResults.get(testName);

    // If there were no other statuses than started and finished then the actual test gets the succeeded status.
    if (currentStatus == TestStatus.STARTED && status == TestStatus.FINISHED) {
      status = TestStatus.SUCCEEDED;
    }

    status.setPrevious(currentStatus);

    testResults.put(testName, status);
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
    String name = getTestName(description);

    updateTestStatus(name, status);

    LOGGER.info(String.format("%s %s %s", description.getDisplayName(), getTestName(description), status));
  }

  /**
   * Creates the name of a test based on its description and the given arbitrary components.
   * 
   * @param description
   *          The {@link Description description} of the test.
   * @param otherComponents
   *          Any arbitrary string components which will be concatenated and used as a suffix.
   * @return
   */
  private String getTestName(Description description, String... otherComponents) {
    StringBuilder sb = new StringBuilder();

    sb.append(description.getClassName()).append('.').append(description.getMethodName());

    for (String component : otherComponents) {
      sb.append('.').append(component);
    }

    return sb.toString().replaceAll("[^a-zA-Z0-9\\-\\._]+", "-");
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
      for (Entry<String, TestStatus> result : testResults.entrySet()) {
        String outcome = result.getValue().getOutcome();

        List<TestStatus> statuses = new LinkedList<TestStatus>();
        result.getValue().getStatusHistory(statuses);

        if (statuses.contains(TestStatus.ASSUMPTION_FAILED)) {
          outcome = TestStatus.ASSUMPTION_FAILED.getOutcome();
        }

        output.write(String.format("%s: %s\n", outcome, result.getKey()));
      }
    }
  }

  @Override
  public void testRunStarted(Description description) throws Exception {
    LOGGER.info("TEST RUN STARTED");
  }

  @Override
  public void testIgnored(Description description) throws Exception {
    handleEvent(description, TestStatus.IGNORED);

    super.testIgnored(description);
  }

  @Override
  public void testStarted(Description description) throws Exception {
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

    File coverageFile = new File(outputDirectory, getTestName(description, Constants.COVERAGE_FILE_EXT));
      outputDirectory.mkdirs();

    final FileOutputStream localFile = new FileOutputStream(coverageFile);

    final ExecutionDataWriter localWriter = new ExecutionDataWriter(localFile);

    final Socket socket = new Socket(InetAddress.getByName(ADDRESS), PORT);
    final RemoteControlWriter writer = new RemoteControlWriter(socket.getOutputStream());
    final RemoteControlReader reader = new RemoteControlReader(socket.getInputStream());
    reader.setSessionInfoVisitor(localWriter);
    reader.setExecutionDataVisitor(localWriter);

    // Send a dump command and read the response:
    writer.visitDumpCommand(true, true);
    reader.read();

    socket.close();
    localFile.close();

    super.testFinished(description);
  }

  @Override
  public void testRunFinished(Result result) throws Exception {
    LOGGER.info(String.format("TEST RUN FINISHED in %dms (tests=%d ignored=%d failed=%d)", result.getRunTime(), result.getRunCount(), result.getIgnoreCount(), result.getFailureCount()));

    /*
     * Workaround to handles the test that have failed.
     * 
     * @TODO: Find out why the testFailure(Failure) method is not called during the execution of the test suite.
     */
    for (Failure failure : result.getFailures()) {
      handleEvent(failure.getDescription(), TestStatus.FAILED);
    }

    dumpTestResults();

    super.testRunFinished(result);
  }
}