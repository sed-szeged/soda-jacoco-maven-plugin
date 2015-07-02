package hu.sed.soda.tools;

import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * An enumeration for storing TestNG test statuses.
 */
public enum TestNGStatus implements TestStatus {

  /**
   * Indicates that the {@link ITestListener#onTestFailure(ITestResult)} was called.
   */
  FAILURED("failure", "FAIL"),
  /**
   * Indicates that the {@link ITestListener#onTestSkipped(ITestResult)} was called.
   */
  SKIPPED("skipped", "SKIP"),
  /**
   * Indicates that the {@link ITestListener#onTestStart(ITestResult)} was called.
   */
  STARTED("start", "STRT"),
  /**
   * Indicates that the {@link ITestListener#onTestSuccess(ITestResult)} was called.
   */
  SUCCEEDED("success", "PASS"),
  /**
   * Indicates that the {@link ITestListener#onTestFailedButWithinSuccessPercentage(ITestResult)} was called.
   */
  SUCCEEDED_WITHIN_FAILURE_PERCENTAGE("percent", "FPASS");

  /**
   * The name of the root cause event.
   */
  private final String event;
  /**
   * The outcome of the test.
   */
  private final String outcome;

  /**
   * Constructor.
   * 
   * @param event
   *          The name of root cause e.g. "failed".
   * @param outcome
   *          The outcome e.g. "FAIL".
   */
  private TestNGStatus(final String event, final String outcome) {
    this.event = event;
    this.outcome = outcome;
  }
  
  @Override
  public String getEvent() {
    return event;
  }
  
  @Override
  public String getOutcome() {
    return outcome;
  }

  /**
   * Creates a status based on the given test result.
   * 
   * @param result
   *          A {@link ITestResult test result}.
   * 
   * @return A status created based on the status constant of the test result.
   */
  public static TestNGStatus createFrom(ITestResult result) {
    int statusNumber = result.getStatus();

    switch (statusNumber) {
      case ITestResult.FAILURE:
        return FAILURED;
      case ITestResult.SKIP:
        return SKIPPED;
      case ITestResult.STARTED:
        return STARTED;
      case ITestResult.SUCCESS:
        return SUCCEEDED;
      case ITestResult.SUCCESS_PERCENTAGE_FAILURE:
        return SUCCEEDED_WITHIN_FAILURE_PERCENTAGE;
      default:
        throw new IllegalArgumentException(String.format("Number %d is az unknown status number", statusNumber));
    }
  }

}
