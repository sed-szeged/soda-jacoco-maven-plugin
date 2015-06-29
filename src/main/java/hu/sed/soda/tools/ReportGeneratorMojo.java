package hu.sed.soda.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.tools.ExecFileLoader;
import org.jacoco.report.DirectorySourceFileLocator;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.html.HTMLFormatter;
import org.jacoco.report.xml.XMLFormatter;


/**
 * This class handles the coverage report generation process which produces the separate XML coverage files for the different tests.
 */
@Mojo(name = "report", defaultPhase = LifecyclePhase.TEST)
public class ReportGeneratorMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project.build.directory}/jacoco")
  private File baseDirectory;

  @Parameter(defaultValue = "${project.build.directory}/jacoco/coverage/raw")
  private File inputDirectory;

  @Parameter(defaultValue = "${project.build.directory}/jacoco/coverage/xml")
  private File outputDirectory;

  @Parameter(defaultValue = "${project.build.directory}/classes")
  private File classesDirectory;

  @Parameter(defaultValue = "${project.build.sourceDirectory}")
  private File sourceDirectory;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("Executing JRunner Maven Plugin ...");

    getLog().debug("base = " + baseDirectory.getAbsolutePath());
    getLog().debug("in = " + inputDirectory.getAbsolutePath());
    getLog().debug("out = " + outputDirectory.getAbsolutePath());
    getLog().debug("classes = " + classesDirectory.getAbsolutePath());
    getLog().debug("source = " + sourceDirectory.getAbsolutePath());

    try {
      String[] coverageFilePaths = getCoverageFilePaths(inputDirectory);

      getLog().debug("files = " + coverageFilePaths.length);

      generateReports(coverageFilePaths);

      getLog().info("Reports were generated successfully.");
    } catch (IllegalStateException e) {
      getLog().warn("Skipping report generation because: " + e.getMessage());
    }
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
    outputDirectory.mkdirs();

    for (String path : coverageFilePaths) {
      String testName = path.replaceAll(String.format("\\.%s", Constants.COVERAGE_FILE_EXT), "");
      File outputFile = new File(outputDirectory, testName + ".xml");
      try {
        ExecFileLoader loader = loadExecutionData(new File(inputDirectory, path));
        // Run the structure analyzer on a single class folder to build up
        // the coverage model. The process would be similar if your classes
        // were in a jar file. Typically you would create a bundle for each
        // class folder and each jar you want in your report. If you have
        // more than one bundle you will need to add a grouping node to your
        // report
        final IBundleCoverage bundleCoverage = analyzeStructure(loader, testName);
        createReport(loader, bundleCoverage, outputFile);

      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private ExecFileLoader loadExecutionData(File executionDataFile) throws IOException {
    ExecFileLoader execFileLoader = new ExecFileLoader();
    execFileLoader.load(executionDataFile);

    return execFileLoader;
  }

  private IBundleCoverage analyzeStructure(ExecFileLoader execFileLoader, String testName) throws IOException {
    final CoverageBuilder coverageBuilder = new CoverageBuilder();
    final Analyzer analyzer = new Analyzer(execFileLoader.getExecutionDataStore(), coverageBuilder);

    analyzer.analyzeAll(classesDirectory);

    return coverageBuilder.getBundle(testName);
  }

  private void createReport(ExecFileLoader execFileLoader, final IBundleCoverage bundleCoverage, File outputFile) throws IOException {

    // Create a concrete report visitor based on some supplied
    // configuration. In this case we use the defaults
    final XMLFormatter xmlFormatter = new XMLFormatter();
    final FileOutputStream out = new FileOutputStream(outputFile);
    final IReportVisitor visitor = xmlFormatter.createVisitor(out);
    final HTMLFormatter htmlFormatter = new HTMLFormatter();

    visitor.visitInfo(execFileLoader.getSessionInfoStore().getInfos(),
    execFileLoader.getExecutionDataStore().getContents());

    // Populate the report structure with the bundle coverage information.
    // Call visitGroup if you need groups in your report.
    visitor.visitBundle(bundleCoverage, new DirectorySourceFileLocator(sourceDirectory, "utf-8", 4));

    // Signal end of structure information to allow report to write all
    // information out
    visitor.visitEnd();

  }
}
