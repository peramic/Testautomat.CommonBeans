package havis.test.suite.beans.creator;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;

import havis.test.suite.api.NDIContext;
import havis.test.suite.api.StatisticCreator;
import havis.test.suite.common.IO;
import havis.test.suite.common.messaging.XMLMessage;
import havis.test.suite.common.messaging.XQuery;

public class Filter implements StatisticCreator {

	private static final Logger log = LoggerFactory.getLogger(Filter.class);

	private static final URI uri = createURI();

	private static URI createURI() {
		try {
			return new URI("http://www.HARTING.com");
		} catch (Exception e) {
			return null;
		}
	}

	private final String extension = ".xml";

	private static final Object lockObject = new Object();

	private static long count;

	private String outputDir;
	private XQuery xqueryReportData;
	private XQuery xqueryExpression;
	private final List<String> filteredReports = new ArrayList<String>();

	private ApplicationContext applicationContext;

	private String expression;
	private boolean reportIfEmpty;

	private Long fileCount;
	private Long directorySize;
	private Long fileLastWriteTimeInterval;

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	public boolean isReportIfEmpty() {
		return reportIfEmpty;
	}

	public void setReportIfEmpty(boolean reportIfEmpty) {
		this.reportIfEmpty = reportIfEmpty;
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

	public class Result {
		private String testCaseName;

		public String getTestCaseName() {
			return testCaseName;
		}

		public void setTestCaseName(String testCaseName) {
			this.testCaseName = testCaseName;
		}
	}

	public Filter() {
		reportIfEmpty = true;
	}

	/**
	 * See
	 * {@link Havis.RfidTestSuite.Interfaces.Reporter#start(NDIContext, String, String)}
	 */
	@Override
	public void start(NDIContext context, String moduleHome, String outputDir)
			throws ConfigurationException, Exception {
		if (expression == null || expression.isEmpty()) {
			throw new ConfigurationException(
					"The module property 'expression' is missed");
		}
		this.outputDir = outputDir;
		xqueryReportData = new XQuery(
				"<result>"
						+ "<testCaseName>{ /*:testCase/*:report/data(@name) }</testCaseName>"
						+ "</result>");
		xqueryExpression = new XQuery(expression);
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
	public void report(String report) throws Exception {
		synchronized (lockObject) {
			String filteredReport = xqueryReportData.execute(report, uri);

			XMLMessage fileData = new XMLMessage(filteredReport);
			Document dom = fileData.getDomDocument();
			Result resSet = new Result();
			resSet.setTestCaseName(dom.getElementsByTagName("testCaseName")
					.item(0).getTextContent());

			filteredReport = xqueryExpression.execute(report, uri);
			filteredReport = removeXMLHeader(filteredReport);

			if (filteredReport.indexOf("<") >= 0) {
				log.info("Found statistic data in report for test case "
						+ resSet.testCaseName);

				if (log.isDebugEnabled()) {
					log.debug(filteredReport);
				}
				filteredReports.add(filteredReport);
			}
		}
	}

	/**
	 * See {@link Havis.RfidTestSuite.Interfaces.StatisticCreator#create()}
	 */
	@Override
	public void create() throws IOException {

		if (filteredReports.size() == 0 && !reportIfEmpty) {
			return;
		}

		StringBuilder report = new StringBuilder(
				"<testCases xmlns=\"http://www.HARTING.com/RFID/TestAutomat/Filter\">"
						+ System.getProperty("line.separator"));

		for (String filteredReport : filteredReports) {
			report.append(removeXMLHeader(filteredReport)
					+ System.getProperty("line.separator"));
		}
		report.append("</testCases>");

		SimpleDateFormat formatter = new SimpleDateFormat(
				"yyyy-MM-dd'T'HH-mm-ss.SSS");
		Date current = new Date();
		String date = formatter.format(current);
		String fileNameBase = date;

		File file1 = new File(outputDir);
		File file = new File(file1, fileNameBase + extension);

		if (java.nio.file.Files.exists(Paths.get(file.toURI()))) {
			file = new File(file1, fileNameBase + "_" + ++count + extension);
		}

		if (java.nio.file.Files.notExists(Paths.get(outputDir))) {
			File dir = new File(outputDir);
			dir.mkdir();
		}

		log.info("Writing statistic to " + file);

		if (log.isDebugEnabled()) {
			log.debug(report.toString());
		}

		new IO(applicationContext).writeResource(file.getAbsolutePath(),
				report.toString());
	}

	/**
	 * Removes the XML header from a XML document.
	 * 
	 * @param xml
	 * @return
	 */
	private static String removeXMLHeader(String xml) {
		int i = xml.indexOf("?>");
		return i >= 0 && xml.length() > 2 ? xml.substring(i + 2) : xml;
	}

}
