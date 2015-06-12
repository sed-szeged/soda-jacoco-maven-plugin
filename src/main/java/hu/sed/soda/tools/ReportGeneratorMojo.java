package hu.sed.soda.tools;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;

import com.vladium.emma.IAppConstants;
import com.vladium.emma.report.ReportProcessor;
import com.vladium.util.XProperties;

/**
 * This class handles the coverage report generation process which produces the separate XML coverage files for the different tests.
 */
@Mojo(name = "report", defaultPhase = LifecyclePhase.TEST)
public class ReportGeneratorMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project.build.directory}/coverage.em")
  private File metaDataFile;

  @Parameter(defaultValue = "${project.build.directory}/emma")
  private File baseDirectory;

  @Parameter(defaultValue = "${project.build.directory}/emma/coverage/raw")
  private File inputDirectory;

  @Parameter(defaultValue = "${project.build.directory}/emma/coverage/xml")
  private File outputDirectory;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("Executing JRunner Maven Plugin ...");

    getLog().debug("meta = " + metaDataFile.getAbsolutePath());
    getLog().debug("base = " + baseDirectory.getAbsolutePath());
    getLog().debug("in = " + inputDirectory.getAbsolutePath());
    getLog().debug("out = " + outputDirectory.getAbsolutePath());

    String[] coverageFilePaths = getCoverageFilePaths(inputDirectory);

    getLog().debug("files = " + coverageFilePaths.length);

    generateReports(coverageFilePaths);

    getLog().info("Reports were generated successfully.");
  }

  /**
   * Collects the coverage files which were produced by EMMA.
   * 
   * @param baseDir
   *          The root directory of the search.
   * 
   * @return The list of coverage file paths relative to baseDir.
   */
  private String[] getCoverageFilePaths(File baseDir) {
    DirectoryScanner scanner = new DirectoryScanner();

    scanner.setBasedir(baseDir.getAbsoluteFile());
    scanner.setIncludes(new String[] { String.format("*.%s", Constants.COVERAGE_FILE_EXT) });
    scanner.scan();

    return scanner.getIncludedFiles();
  }

  /**
   * Generates the XML report files based on the separate coverage files. EMMA's {@link ReportProcessor} is used to generate the output.
   * 
   * @param coverageFilePaths
   *          The coverage data files.
   */
  private void generateReports(String[] coverageFilePaths) {
    int numOfFiles = coverageFilePaths.length;

    ReportProcessor reporter = ReportProcessor.create();

    reporter.setAppName(IAppConstants.APP_NAME);
    reporter.setReportTypes(new String[] { "xml" });

    for (int i = 0; i <= numOfFiles; ++i) {
      File outputFile = null;
      List<String> dataPaths = new LinkedList<String>();

      // The meta data file should always be added to data paths.
      dataPaths.add(metaDataFile.getAbsolutePath());

      if (i == numOfFiles) { // Generating a common report using all coverage data files.
        outputFile = new File(baseDirectory, "coverage.xml");

        for (String path : coverageFilePaths) {
          File file = new File(inputDirectory, path);

          dataPaths.add(file.getAbsolutePath());
        }
      } else { // Generating separate reports using only one coverage data file.
        outputFile = new File(outputDirectory, coverageFilePaths[i].replaceAll(String.format("\\.%s", Constants.COVERAGE_FILE_EXT), "\\.xml"));

        File file = new File(inputDirectory, coverageFilePaths[i]);

        dataPaths.add(file.getAbsolutePath());
      }

      reporter.setDataPath(dataPaths.toArray(new String[] {}));

      XProperties properties = new XProperties();
      properties.setProperty("report.xml.out.file", outputFile.getAbsolutePath());
      reporter.setPropertyOverrides(properties);

      reporter.run();
    }
  }

}
