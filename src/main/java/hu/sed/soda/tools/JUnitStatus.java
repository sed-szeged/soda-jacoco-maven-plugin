package hu.sed.soda.tools;

import org.junit.runner.notification.RunListener;

/**
 * An enumeration for storing JUnit test statuses.
 */
public enum JUnitStatus implements TestStatus {

  /**
   * Indicates that {@link RunListener#testRunStarted(org.junit.runner.Description)} was called.
   */
  STARTED("started", "STRT"),
  /**
   * Indicates that {@link RunListener#testIgnored(org.junit.runner.Description)} was called.
   */
  IGNORED("ignored", "IGNR"),
  /**
   * Indicates that {@link RunListener#testFailure(org.junit.runner.notification.Failure)} was called.
   */
  FAILED("failed", "FAIL"),
  /**
   * Indicates that {@link RunListener#testAssumptionFailure(org.junit.runner.notification.Failure)} was called.
   */
  ASSUMPTION_FAILED("assumption failed", "AFAIL"),
  /**
   * Indicates that {@link RunListener#testFinished(org.junit.runner.Description)} was called.
   */
  FINISHED("finished", "FNSH"),
  /**
   * Indicates that the test finished successfully i.e. there were no other statuses than {@link #STARTED} and {@link #FINISHED}.
   */
  SUCCEEDED("succeeded", "PASS");

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
  private JUnitStatus(final String event, final String outcome) {
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

}
