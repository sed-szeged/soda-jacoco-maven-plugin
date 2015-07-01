package hu.sed.soda.tools;

import java.nio.file.Path;
import java.nio.file.Paths;

public interface Constants {

  /**
   * The address of the JaCoCo agent.
   */
  static final String JACOCO_AGENT_ADDRESS = "localhost";

  /**
   * The port on which the JaCoCo agent listens.
   */
  static final int JACOCO_AGENT_PORT = 9999;

  /**
   * The extension of coverage files produced by EMMA.
   */
  static final String COVERAGE_FILE_EXT = "exec";

  /**
   * The default directory.
   */
  static final String BASE_DIR = "target/jacoco";

  /**
   * The directory for coverage data.
   */
  static final Path COVERAGE_DIR = Paths.get(BASE_DIR, "coverage", "raw");

}