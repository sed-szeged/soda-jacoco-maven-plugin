package hu.sed.soda.tools;

/**
 * Common interface for JUnit and TestNG test statuses.
 */
public interface TestStatus {

  /**
   * @return The name of the event which caused the actual status.
   */
  public String getEvent();

  /**
   * @return The outcome of the test.
   */
  public String getOutcome();

}
