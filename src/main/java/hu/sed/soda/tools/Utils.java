package hu.sed.soda.tools;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.junit.runner.Description;
import org.testng.ITestResult;

import com.vladium.emma.EMMARuntimeException;
import com.vladium.emma.ctl.ControlRequest;
import com.vladium.emma.ctl.CtlProcessor;

/**
 * This class contains commonly used methods.
 */
public class Utils {

  private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());

  /**
   * Creates the name of a test based on its description.
   * 
   * @param description
   *          The {@link Description description} of the test.
   * @return The name of the test.
   */
  public static String getTestName(Description description) {
    StringBuilder sb = new StringBuilder();

    sb.append(description.getClassName()).append('.').append(description.getMethodName());

    return postProcessTestName(sb.toString());
  }

  /**
   * Creates the name of a test based on a test result.
   * 
   * @param description
   *          The {@link ITestResult test result}.
   * @return The name of the test.
   */
  public static String getTestName(ITestResult result) {
    StringBuilder sb = new StringBuilder();

    sb.append(result.getInstanceName()).append('.').append(result.getName());

    return postProcessTestName(sb.toString());
  }

  /**
   * Replaces unwanted characters in the name of a test.
   * 
   * @param testName
   *          The name of a test.
   * 
   * @return The post-processed test name.
   */
  private static String postProcessTestName(String testName) {
    return testName.replaceAll("[^a-zA-Z0-9\\-\\._]+", "-");
  }

  /**
   * Saves then resets the actual coverage.
   * 
   * @param coverageFile
   *          The file in which the coverage data should be stored.
   */
  public static void dumpAndResetCoverage(File coverageFile) {
    try {
      /*
       * This code manages the coverage data by calling the ctl tool of EMMA. See more at http://sourceforge.net/p/emma/news/2005/06/emma-early-access-build-215320-available-new-ctl-tool/
       */
      List<ControlRequest> requests = new LinkedList<ControlRequest>();
      requests.add(ControlRequest.create(ControlRequest.COMMAND_DUMP_COVERAGE, new String[] { coverageFile.getAbsolutePath(), "false", "false" }));
      requests.add(ControlRequest.create(ControlRequest.COMMAND_RESET_COVERAGE, null));

      CtlProcessor processor = CtlProcessor.create();
      processor.setCommandSequence(requests.toArray(new ControlRequest[] {}));
      processor.run();
    } catch (EMMARuntimeException e) {
      LOGGER.warning("Cannot dump and reset coverage because: " + e.getMessage());
    }
  }

}
