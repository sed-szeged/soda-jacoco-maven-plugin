package hu.sed.soda.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Logger;

import org.jacoco.core.data.ExecutionDataWriter;
import org.jacoco.core.runtime.RemoteControlReader;
import org.jacoco.core.runtime.RemoteControlWriter;
import org.junit.runner.Description;
import org.testng.ITestResult;

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
      final FileOutputStream localFile = new FileOutputStream(coverageFile);

      final ExecutionDataWriter localWriter = new ExecutionDataWriter(localFile);

      final Socket socket = new Socket(InetAddress.getByName(Constants.JACOCO_AGENT_ADDRESS), Constants.JACOCO_AGENT_PORT);
      final RemoteControlWriter writer = new RemoteControlWriter(socket.getOutputStream());
      final RemoteControlReader reader = new RemoteControlReader(socket.getInputStream());
      reader.setSessionInfoVisitor(localWriter);
      reader.setExecutionDataVisitor(localWriter);

      // Send a dump command and read the response:
      writer.visitDumpCommand(true, true);
      reader.read();

      socket.close();
      localFile.close();
    } catch (IOException e) {
      LOGGER.warning("Cannot dump and reset coverage because: " + e.getMessage());
    }
  }

}
