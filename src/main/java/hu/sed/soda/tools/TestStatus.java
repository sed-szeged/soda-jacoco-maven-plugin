package hu.sed.soda.tools;

import java.util.List;

import org.junit.runner.notification.RunListener;

/**
 * An enumeration for storing test test statuses.
 */
public enum TestStatus {

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
   * The previous status of the test.
   */
  private TestStatus previous = null;

  /**
   * Constructor.
   * 
   * @param event
   *          The name of root cause e.g. "failed".
   * @param outcome
   *          The outcome e.g. "FAIL".
   */
  private TestStatus(final String event, final String outcome) {
    this.event = event;
    this.outcome = outcome;
  }

  public String getEvent() {
    return event;
  }

  public String getOutcome() {
    return outcome;
  }

  public TestStatus getPrevious() {
    return previous;
  }

  public void setPrevious(TestStatus previous) {
    this.previous = previous;
  }

  @Override
  public String toString() {
    return this.event;
  }

  /**
   * Retrieves the actual and previous statuses recursively.
   * 
   * @param list
   *          A list which will be populated with the statuses. The latest status will be the last item of this list.
   */
  public void getStatusHistory(List<TestStatus> list) {
    if (previous != null) {
      previous.getStatusHistory(list);
    }

    list.add(this);
  }

}
