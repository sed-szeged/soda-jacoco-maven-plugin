package hu.sed.soda.tools;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.tools.ExecFileLoader;
import org.jacoco.report.DirectorySourceFileLocator;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.xml.XMLFormatter;

/**
 * Handles the coverage report generation process which produces the separate XML coverage files for the different tests.
 */
@Mojo(name = "report")
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
  
  /**
   * The revision identifier of the actual program under test.
   */
  @Parameter(defaultValue = "0")
  private String revision;

  /**
   * Associates the hash of the name and the full name of a test together.
   */
  private Map<String, String> hashToTestMap = new HashMap<String, String>();

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("Executing SoDA Maven Plugin ...");

    getLog().debug("base = " + baseDirectory.getAbsolutePath());
    getLog().debug("in = " + inputDirectory.getAbsolutePath());
    getLog().debug("out = " + outputDirectory.getAbsolutePath());
    getLog().debug("classes = " + classesDirectory.getAbsolutePath());
    getLog().debug("source = " + sourceDirectory.getAbsolutePath());

    try {
      outputDirectory.mkdirs();

      String[] coverageFilePaths = getCoverageFilePaths(inputDirectory);

      getLog().debug("files = " + coverageFilePaths.length);

      generateReports(coverageFilePaths);

      getLog().info("Reports were generated successfully.");
    } catch (IllegalStateException | IOException e) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      e.printStackTrace(new PrintStream(baos));

      getLog().warn("Skipping report generation because: " + baos.toString());
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
   * 
   * @throws IOException
   * @throws FileNotFoundException
   */
  private void generateReports(String[] coverageFilePaths) throws FileNotFoundException, IOException {    
    getLog().info("Generating reports...");

    createHashToTestMapping();
    
    int index = 0;
    final int numOfPaths = coverageFilePaths.length;
    final int stepSize = Math.max(1, numOfPaths / 10);

    for (String path : coverageFilePaths) {
      String nameHash = path.replaceAll(String.format("\\.%s", Constants.COVERAGE_FILE_EXT), "");

      ExecFileLoader loader = loadExecutionData(new File(inputDirectory, path));

      // Run the structure analyzer on a single class folder to build up the coverage model.
      // The process would be similar if your classes were in a jar file.
      // Typically you would create a bundle for each class folder and each jar you want in your report.
      // If you have more than one bundle you will need to add a grouping node to your report.
      final IBundleCoverage bundleCoverage = analyzeStructure(loader, nameHash);

      createReport(loader, bundleCoverage, nameHash);
      
      if (++index % stepSize == 0) {
        getLog().info(String.format("%d%% done.", 100 * index / numOfPaths));
      }
    }
  }

  /**
   * Loads coverage data from a given file.
   * 
   * @param executionDataFile
   *          An arbitrary .exec file.
   * 
   * @return An {@link ExecFileLoader} that holds the coverage data.
   * 
   * @throws IOException
   */
  private ExecFileLoader loadExecutionData(File executionDataFile) throws IOException {
    ExecFileLoader execFileLoader = new ExecFileLoader();
    execFileLoader.load(executionDataFile);

    return execFileLoader;
  }

  /**
   * Creates a coverage bundle by analyzing the given {@link ExecFileLoader}.
   * 
   * @param execFileLoader
   *          An arbitrary {@link ExecFileLoader} that holds the coverage data.
   * @param testName
   *          The name of a test which will be used as the bundle name.
   * 
   * @return A coverage {@link IBundleCoverage bundle}.
   * 
   * @throws IOException
   */
  private IBundleCoverage analyzeStructure(ExecFileLoader execFileLoader, String testName) throws IOException {
    final CoverageBuilder coverageBuilder = new CoverageBuilder();
    final Analyzer analyzer = new Analyzer(execFileLoader.getExecutionDataStore(), coverageBuilder);

    analyzer.analyzeAll(classesDirectory);

    return coverageBuilder.getBundle(testName);
  }

  /**
   * Creates report files based on the given coverage information.
   * 
   * @param execFileLoader
   *          An arbitrary {@link ExecFileLoader} that holds the coverage data.
   * @param bundleCoverage
   *          A coverage {@link IBundleCoverage bundle}.
   * @param testNameHash
   *          The hash of the name of a test.
   * 
   * @throws IOException
   */
  private void createReport(ExecFileLoader execFileLoader, final IBundleCoverage bundleCoverage, String testNameHash) throws IOException {
    final File outputFile = new File(outputDirectory, testNameHash + ".xml");
    FileOutputStream out = new FileOutputStream(outputFile);

    // Create a concrete report visitor based on some supplied configuration. In this case we use the defaults
    final XMLFormatter xmlFormatter = new XMLFormatter();
    xmlFormatter.setOutputEncoding("UTF-8");

    final IReportVisitor visitor = xmlFormatter.createVisitor(out);
    visitor.visitInfo(execFileLoader.getSessionInfoStore().getInfos(), execFileLoader.getExecutionDataStore().getContents());

    // Populate the report structure with the bundle coverage information.
    // Call visitGroup if you need groups in your report.
    visitor.visitBundle(bundleCoverage, new DirectorySourceFileLocator(sourceDirectory, "UTF-8", 4));

    // Signal end of structure information to allow report to write all information out
    visitor.visitEnd();

    // Appending the full name of the actual test to the end of the output file.
    out = new FileOutputStream(outputFile, true);

    out.write(String.format("<!-- %s -->", hashToTestMap.get(testNameHash)).getBytes());
    out.close();
  }

  /**
   * Reads the map file which is placed beside the .exec files and initializes the mapping which associates the full test names with their hashes.
   * 
   * @throws IOException
   * @throws FileNotFoundException
   */
  public void createHashToTestMapping() throws IOException, FileNotFoundException {
    File mapFile = Paths.get(baseDirectory.getPath(), revision, String.format("%s.r%s", Constants.MAP_FILE, revision)).toFile();

    try (BufferedReader input = new BufferedReader(new FileReader(mapFile))) {
      String line = null;

      while ((line = input.readLine()) != null) {
        String[] tokens = line.split(Constants.MAP_FILE_SEPARATOR);

        hashToTestMap.put(tokens[0], tokens[1]);
      }
    }
  }

}
