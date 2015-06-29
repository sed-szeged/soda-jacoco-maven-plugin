package hu.sed.soda.tools;

import java.util.LinkedList;
import java.util.List;

/**
 * Stores information about a test.
 */
public class TestInfo {

  /**
   * This index is used for avoiding test name collisions.
   */
  private static long globalIndex = 0;

  /**
   * The index of the test.
   */
  private long index;

  /**
   * The name of the test.
   */
  private String testName;

  /**
   * The statuses of the test.
   */
  List<TestStatus> statuses;

  /**
   * Creates a test information object.
   * 
   * @param testName The name of the test.
   */
  public TestInfo(String testName) {
    this.index = globalIndex++;
    this.testName = testName;
    this.statuses = new LinkedList<TestStatus>();
  }

  public List<TestStatus> getStatuses() {
    return statuses;
  }

  /**
   * Calculates the final status of a test based on the list of statuses that occurred during the execution of the test.
   * 
   * @return The final status.
   */
  public TestStatus getFinalStatus() {
    TestStatus status = TestStatus.SUCCEEDED;

    if (statuses.contains(TestStatus.IGNORED)) {
      status = TestStatus.IGNORED;
    } else if (statuses.contains(TestStatus.ASSUMPTION_FAILED)) {
      status = TestStatus.ASSUMPTION_FAILED;
    } else if (statuses.contains(TestStatus.FAILED)) {
      status = TestStatus.FAILED;
    }

    return status;
  }

  /**
   * Creates a name for the actual test.
   * 
   * @return The name of the test concatenated with the index of the test.
   */
  public String getFullTestName() {
    return testName + '.' + index;
  }

  /**
   * Adds a status to the list of statuses.
   * 
   * @param status An arbitrary test status.
   */
  public void addStatus(TestStatus status) {
    statuses.add(status);
  }

}
