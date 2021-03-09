package havis.test.suite.beans.reporter;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;

import net.sf.saxon.s9api.SaxonApiException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import havis.test.suite.api.NDIContext;
import havis.test.suite.api.Reporter;
import havis.test.suite.common.IO;
import havis.test.suite.common.messaging.XMLMessage;
import havis.test.suite.common.messaging.XMLNormalizationException;
import havis.test.suite.common.messaging.XQuery;

public class Files implements Reporter {

	private static final Logger log = LoggerFactory.getLogger(Reporter.class);
	private static final URI uri = createURI();

	private static URI createURI() {
		try {
			return new URI("http://www.HARTING.com");
		} catch (Exception e) {
			return null;
		}
	}

	private static final Object lockObject = new Object();
	private static long count;

	private final String extension = ".xml";

	private String outputDir;
	private XQuery xqueryReportData;

	private ApplicationContext applicationContext;

	private Long fileCount;
	private Long directorySize;
	private Long fileLastWriteTimeInterval;

	public class Result {
		private String testCaseName;
		private boolean isFullReport;

		public String getTestCaseName() {
			return testCaseName;
		}

		public void setTestCaseName(String testCaseName) {
			this.testCaseName = testCaseName;
		}

		public boolean getIsFullReport() {
			return isFullReport;
		}

		public void setIsFullReport(boolean isFullReport) {
			this.isFullReport = isFullReport;
		}
	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public Long getFileCount() {
		return fileCount;
	}

	public void setFileCount(Long fileCount) {
		this.fileCount = fileCount;
	}

	public Long getDirectorySize() {
		return directorySize;
	}

	public void setDirectorySize(Long directorySize) {
		this.directorySize = directorySize;
	}

	public Long getFileLastWriteTimeInterval() {
		return fileLastWriteTimeInterval;
	}

	public void setFileLastWriteTimeInterval(Long fileLastWriteTimeInterval) {
		this.fileLastWriteTimeInterval = fileLastWriteTimeInterval;
	}

	/**
	 * See
	 * {@link Havis.RfidTestSuite.Interfaces.Reporter#start(NDIContext, String, String)}
	 */
	@Override
	public void start(NDIContext context, String moduleHome, String outputDir)
			throws InterruptedException, SaxonApiException {
		this.outputDir = outputDir;
		xqueryReportData = new XQuery(
				"<result>"
						+ "<testCaseName>{ /*:testCase/*:report/data(@name) }</testCaseName>"
						+ "<isFullReport>{ exists(/*:testCase/*:report/@endTime) }</isFullReport>"
						+ "</result>");
	}

	/**
	 * See {@link Havis.RfidTestSuite.Interfaces.Reporter#stop()}
	 */
	@Override
	public void stop() throws IOException {
		cleanup();
	}

	/**
	 * See {@link Havis.RfidTestSuite.Interfaces.Reporter#cleanup()}
	 */
	@Override
	public void cleanup() throws IOException {
		if (fileCount != null || directorySize != null
				|| fileLastWriteTimeInterval != null) {
			int fileCount = new IO(applicationContext).deleteFiles(outputDir,
					"*.xml", this.fileCount, directorySize, new Date(),
					fileLastWriteTimeInterval);
			if (fileCount > 0) {
				log.info("Deleted " + fileCount + " file"
						+ (fileCount > 1 ? "s" : ""));
			}
		}
	}

	/**
	 * See {@link Havis.RfidTestSuite.Interfaces.Reporter#report(String)}
	 */
	@Override
	public void report(String report) throws IOException, SaxonApiException,
			XMLNormalizationException, ParserConfigurationException,
			SAXException {
		// get report data
		String filteredReport = xqueryReportData.execute(report, uri);
		XMLMessage fileData = new XMLMessage(filteredReport);
		Document dom = fileData.getDomDocument();
		Result resSet = new Result();
		resSet.setTestCaseName(dom.getElementsByTagName("testCaseName").item(0)
				.getTextContent());
		resSet.setIsFullReport(false);
		if (dom.getElementsByTagName("isFullReport").item(0).getTextContent()
				.equals("true")) {
			resSet.setIsFullReport(true);
		}

		// create file name and its directory
		SimpleDateFormat formatter = new SimpleDateFormat(
				"yyyy-MM-dd'T'HH-mm-ss.SSS");
		Date current = new Date();
		String date = formatter.format(current);
		String fileNameBase = resSet.getTestCaseName() + "_" + date;
		// extend file name for intermediate reports
		if (!resSet.isFullReport) {
			fileNameBase += "_intermediate";
		}
		File file1 = new File(outputDir);
		File file = new File(file1, fileNameBase + extension);
		synchronized (lockObject) {
			if (java.nio.file.Files.exists(Paths.get(file.toURI()))) {
				// create file name with counter
				file = new File(file1, fileNameBase + "_" + ++count + extension);
			}
			// create the directory if it does not exist
			if (java.nio.file.Files.notExists(Paths.get(outputDir))) {
				File dir = new File(outputDir);
				dir.mkdir();
			}

			// create file
			log.info("Writing report to " + file);
			if (log.isDebugEnabled()) {
				log.debug(report);
			}
			new IO(applicationContext).writeResource(file.getAbsolutePath(),
					report);
		}
	}

}
