package hu.sed.soda.tools;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * Stores information about a test.
 */
public class TestInfo {

  /**
   * The name of the test.
   */
  private String testName;

  /**
   * The statuses of the test.
   */
  List<TestStatus> statuses;

  /**
   * Whether the status of the test is concrete or it should be calculated.
   */
  private boolean isConcrete;

  /**
   * Creates a test information object.
   * 
   * @param testName
   *          The name of the test.
   */
  public TestInfo(String testName) {
    this.testName = testName;
    this.statuses = new LinkedList<TestStatus>();
    this.isConcrete = false;
  }

  /**
   * Creates a test information object with an initial status.
   * 
   * @param testName
   *          The name of the test.
   * @param status
   *          The status of the test.
   */
  public TestInfo(String testName, TestStatus status) {
    this(testName);

    this.statuses.add(status);
    this.isConcrete = true;
  }

  public String getTestName() {
    return testName;
  }

  public List<TestStatus> getStatuses() {
    return statuses;
  }

  /**
   * @return The hash of the test info.
   */
  public String getHash() {
    return DigestUtils.md5Hex(testName);
  }

  /**
   * Calculates the final status of a test based on the list of statuses that occurred during the execution of the test.
   * 
   * @return The final status.
   */
  public TestStatus getFinalStatus() {
    TestStatus status = null;

    if (isConcrete) {
      status = statuses.get(0);
    } else {
      status = JUnitStatus.SUCCEEDED;

      if (statuses.contains(JUnitStatus.IGNORED)) {
        status = JUnitStatus.IGNORED;
      } else if (statuses.contains(JUnitStatus.ASSUMPTION_FAILED)) {
        status = JUnitStatus.ASSUMPTION_FAILED;
      } else if (statuses.contains(JUnitStatus.FAILED)) {
        status = JUnitStatus.FAILED;
      }
    }

    return status;
  }

  /**
   * Adds a status to the list of statuses.
   * 
   * @param status
   *          An arbitrary test status.
   */
  public void addStatus(JUnitStatus status) {
    statuses.add(status);
  }

}
