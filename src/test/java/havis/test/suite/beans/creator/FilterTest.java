package havis.test.suite.beans.creator;

import havis.test.suite.api.StatisticCreator;
import havis.test.suite.common.helpers.DateHelper;
import havis.test.suite.common.helpers.FileHelper;
import havis.test.suite.common.helpers.PathResolverFileHelper;
import havis.test.suite.common.ndi.SynchronizedNDIContext;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class FilterTest {
	private Path moduleHome = null;
	private final String creatorBeanFolder = "modules" + File.separator
			+ "Havis.RfidTestSuite.StatisticsCommon";
	private final String objectsFile = "beans.xml";
	// Output directory in clathpath (Point Seperated relative to classpath)
	private String outputDir = "output";
	private GenericApplicationContext objContext;

	/**
	 * 
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public FilterTest() throws IOException, URISyntaxException {
		List<Path> basePathes = null;
		// Get Paths of classpaths
		basePathes = PathResolverFileHelper.getAbsolutePathFromResource("test");
		if (basePathes != null && basePathes.size() > 0) {
			moduleHome = basePathes.get(0);
		}
		basePathes = PathResolverFileHelper.getAbsolutePathFromResource(
				outputDir.replace(".", File.separator));
		if (basePathes != null && basePathes.size() > 0) {
			outputDir = basePathes.get(0).toString();
		}
	}

	/**
	 * 
	 * @throws IOException
	 */
	@BeforeClass
	public void setUp() throws IOException {
		if (moduleHome != null && outputDir != null) {
			// get objects via spring framework
			objContext = new GenericApplicationContext();
			XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(
					objContext);
			Path plPath = Paths.get(moduleHome + File.separator
					+ creatorBeanFolder + File.separator + objectsFile);
			reader.loadBeanDefinitions("file:///" + plPath);
			objContext.refresh();
			// delete output dir
			FileHelper.deleteFiles(outputDir);
		}
	}

	/**
	 * 
	 * @throws Exception
	 */
	@Test
	public void create() throws Exception {
		// start statistic creater without valid configuration
		// expected: ConfigurationException
		StatisticCreator creator = (StatisticCreator) objContext
				.getBean("statistics.create.noConf");
		try {
			creator.start(new SynchronizedNDIContext(), moduleHome.toString(),
					outputDir);
			Assert.fail();
		} catch (ConfigurationException e) {
			Assert.assertTrue(e.getMessage().contains("missed"));
		} catch (Exception e) {
			Assert.fail();
		}

		// do not send a report
		// expected: no statistic file
		creator = (StatisticCreator) objContext.getBean("statistics.create.a");
		creator.start(new SynchronizedNDIContext(), moduleHome.toString(),
				outputDir);
		creator.create();
		creator.stop();
		Assert.assertTrue(new File(outputDir).list().length == 0);

		// do not send a report but expect a statistic file
		// expected: statistic file without reports
		creator = (StatisticCreator) objContext.getBean("statistics.create.b");
		creator.start(new SynchronizedNDIContext(), moduleHome.toString(),
				outputDir);
		String start = DateHelper.getDateString(new Date()) + ".xml";
		creator.create();
		String end = DateHelper.getDateString(new Date()) + ".xml";
		creator.stop();
		File files = new File(outputDir);
		Assert.assertEquals(files.list().length, 1);
		// check file name of statistic file
		String file = files.list()[0].toString();
		Assert.assertTrue(start.compareTo(file) <= 0);
		Assert.assertTrue(file.compareTo(end) <= 0);
		// check content of statistic file
		String content = FileHelper.readFile(outputDir + File.separator + file);
		Assert.assertEquals(content,
				"<testCases xmlns=\"http://www.HARTING.com/RFID/TestAutomat/Filter\">"
						+ System.getProperty("line.separator") + ""
						+ "</testCases>");
		FileHelper.deleteFiles(outputDir);

		// send reports
		// expected: statistic report file with valid file name and content
		creator = (StatisticCreator) objContext.getBean("statistics.create.a");
		creator.start(new SynchronizedNDIContext(), moduleHome.toString(),
				outputDir);
		creator.report("<testCase><a>1</a></testCase>");
		creator.report("<testCase><a>2</a></testCase>");
		start = DateHelper.getDateString(new Date()) + ".xml";
		creator.create();
		end = DateHelper.getDateString(new Date()) + ".xml";
		creator.stop();
		files = new File(outputDir);
		Assert.assertEquals(files.list().length, 1);
		// check file name of statistic file
		file = files.list()[0].toString();
		Assert.assertTrue(start.compareTo(file) <= 0);
		Assert.assertTrue(file.compareTo(end) <= 0);
		content = FileHelper.readFile(outputDir + File.separator + file);
		Assert.assertEquals(
				content,
				"<testCases xmlns=\"http://www.HARTING.com/RFID/TestAutomat/Filter\">"
						+ System.getProperty("line.separator") + ""
						+ "<a>1</a>" + System.getProperty("line.separator")
						+ "" + "<a>2</a>"
						+ System.getProperty("line.separator") + ""
						+ "</testCases>");
		FileHelper.deleteFiles(outputDir);
	}

	@Test
	public void errors() throws Exception {
		final boolean intermediate = true;
		final boolean inclVerificationError = true;
		final boolean inclException = true;

		// send report without errors
		StatisticCreator creator = (StatisticCreator) objContext
				.getBean("statistics.errors");
		creator.start(new SynchronizedNDIContext(), moduleHome.toString(),
				outputDir);
		final String testCaseName1 = "a";
		creator.report(getReport(!intermediate, testCaseName1,
				!inclVerificationError, !inclException));
		creator.create();
		creator.stop();
		Assert.assertTrue(new File(outputDir).list().length == 0);

		// send reports with errors
		// expected: statistic report file with valid file name and content
		creator = (StatisticCreator) objContext.getBean("statistics.errors");
		creator.start(new SynchronizedNDIContext(), moduleHome.toString(),
				outputDir);
		creator.report(getReport(!intermediate, testCaseName1,
				inclVerificationError, inclException));
		final String testCaseName2 = "b";
		creator.report(getReport(intermediate, testCaseName2,
				inclVerificationError, inclException));
		creator.create();
		creator.stop();
		File files = new File(outputDir);
		Assert.assertEquals(files.list().length, 1);
		String file = files.list()[0];
		// check content of statistic file
		String content = FileHelper.readFile(outputDir + File.separator + file);
		Assert.assertEquals(content, getReportResult(testCaseName1));
		FileHelper.deleteFiles(outputDir);
	}

	@Test
	public void cleanup() throws Exception {
		StatisticCreator creator = (StatisticCreator) objContext
				.getBean("statistics.cleanup.fileLastWriteTimeInterval");
		creator.start(new SynchronizedNDIContext(), moduleHome.toString(),
				outputDir);
		// file String (1100 bytes)
		String buffer = "";
		for (int i = 0; i < 1100; i++) {
			buffer += "a";
		}
		// create 4 dummy reports (suffix ".xml") and 1 foreign file with suffix
		// ".txt"
		FileHelper.waitForFullSeconds();
		for (int i = 0; i < 4; i++) {
			FileHelper.writeFile(
					new File(outputDir, i + ".xml").getAbsolutePath(), buffer);
			Thread.sleep(1000);
		}
		FileHelper.writeFile(new File(outputDir, "x.txt").getAbsolutePath(),
				buffer);
		// delete oldest file "0.xml" using max. time interval = 4 sec
		Thread.sleep(300);// => file "1.xml" was written ~ 3.3 sec before0
		creator.cleanup();
		creator.stop();
		File files = new File(outputDir);
		String[] l = files.list();
		Arrays.sort(l);
		Assert.assertEquals(l.length, 4);
		Assert.assertTrue(l[0].endsWith("1.xml"));
		Assert.assertTrue(l[1].endsWith("2.xml"));
		Assert.assertTrue(l[2].endsWith("3.xml"));
		Assert.assertTrue(l[3].endsWith("x.txt"));

		// delete oldest file "1.xml" using max. count = 2
		creator = (StatisticCreator) objContext
				.getBean("statistics.cleanup.fileCount");
		creator.start(new SynchronizedNDIContext(), moduleHome.toString(),
				outputDir);
		creator.cleanup();
		creator.stop();
		files = new File(outputDir);
		l = files.list();
		Arrays.sort(l);
		Assert.assertEquals(l.length, 3);
		Assert.assertTrue(l[0].endsWith("2.xml"));
		Assert.assertTrue(l[1].endsWith("3.xml"));
		Assert.assertTrue(l[2].endsWith("x.txt"));

		// delete oldest file "2.xml" using max size = 2 kB ("3.xml": 1100
		// bytes)
		creator = (StatisticCreator) objContext
				.getBean("statistics.cleanup.directorySize");
		creator.start(new SynchronizedNDIContext(), moduleHome.toString(),
				outputDir);
		creator.cleanup();
		creator.stop();
		files = new File(outputDir);
		l = files.list();
		Arrays.sort(l);
		Assert.assertEquals(l.length, 2);
		Assert.assertTrue(l[0].endsWith("3.xml"));
		Assert.assertTrue(l[1].endsWith("x.txt"));
		FileHelper.deleteFiles(outputDir);
	}

	private static String getReport(boolean isIntermediate,
			String testCaseName, boolean inclVerificationError,
			boolean inclException) {
		String ret = "<q1:testCase xmlns:q1=\"http://www.HARTING.com/RFID/TestAutomat\">%1$s"
				+ "  <q1:report isIntermediateReport=\"%2$s\" name=\"%3$s\" />%1$s"
				+ "  <q1:steps>";
		if (inclVerificationError) {
			ret += "%1$s    <q1:step name=\"s1\">%1$s"
					+ "      <q1:module>%1$s"
					+ "        <q1:object objectId=\"a\" />%1$s"
					+ "        <q1:verifications>%1$s"
					+ "          <q1:verification name=\"test\">%1$s"
					+ "            <q1:expected>%1$s"
					+ "              <q1:result>&lt;any &gt;</q1:result>%1$s"
					+ "            </q1:expected>%1$s"
					+ "          </q1:verification>%1$s"
					+ "        </q1:verifications>%1$s"
					+ "        <q1:reports>%1$s"
					+ "          <q1:report endTime=\"2012-07-31T06:16:41.641037Z\" startTime=\"2012-07-31T06:16:39.639037Z\">%1$s"
					+ "            <q1:result />%1$s"
					+ "            <q1:verifications>%1$s"
					+ "              <q1:verification name=\"test\">%1$s"
					+ "                <q1:actual />%1$s"
					+ "                <q1:expected>&lt;?xml version=\"1.0\" encoding=\"utf-8\"?&gt;%1$s"
					+ "&lt;any /&gt;</q1:expected>%1$s"
					+ "                <q1:diff>+ ...&lt;?xml version=\"1.0\" e...</q1:diff>%1$s"
					+ "              </q1:verification>%1$s"
					+ "            </q1:verifications>%1$s"
					+ "          </q1:report>%1$s"
					+ "        </q1:reports>%1$s" + "      </q1:module>%1$s"
					+ "    </q1:step>";
		}
		ret += "%1$s    <q1:step name=\"s2\">%1$s" + "      <q1:import>%1$s"
				+ "        <q1:testCaseName>Example2</q1:testCaseName>%1$s"
				+ "      </q1:import>%1$s" + "    </q1:step>";
		if (inclException) {
			ret += "%1$s    <q1:step name=\"s3\">%1$s"
					+ "      <q1:module>%1$s"
					+ "        <q1:object objectId=\"b\" />%1$s"
					+ "        <q1:reports>%1$s"
					+ "          <q1:report endTime=\"2012-07-31T06:16:43.676037Z\" startTime=\"2012-07-31T06:16:43.176037Z\">%1$s"
					+ "            <q1:result isException=\"true\">anyException</q1:result>%1$s"
					+ "          </q1:report>%1$s"
					+ "        </q1:reports>%1$s" + "      </q1:module>%1$s"
					+ "    </q1:step>";
		}
		ret += "%1$s  </q1:steps>%1$s" + "</q1:testCase>";
		return String.format(ret, System.getProperty("line.separator"),
				isIntermediate ? "true" : "false", testCaseName);
	}

	private static String getReportResult(String testCaseName) {
		return String
				.format("<testCases xmlns=\"http://www.HARTING.com/RFID/TestAutomat/Filter\">%1$s"
						+ "<q1:testCase xmlns:q1=\"http://www.HARTING.com/RFID/TestAutomat\">"
						+ "<q1:report isIntermediateReport=\"false\" name=\"a\"/>"
						+ "<q1:steps><q1:step name=\"s1\">%1$s"
						+ "      <q1:module>%1$s"
						+ "        <q1:object objectId=\"a\"/>%1$s"
						+ "        <q1:verifications>%1$s"
						+ "          <q1:verification name=\"test\">%1$s"
						+ "            <q1:expected>%1$s"
						+ "              <q1:result>&lt;any &gt;</q1:result>%1$s"
						+ "            </q1:expected>%1$s"
						+ "          </q1:verification>%1$s"
						+ "        </q1:verifications>%1$s"
						+ "        <q1:reports>%1$s"
						+ "          <q1:report endTime=\"2012-07-31T06:16:41.641037Z\" startTime=\"2012-07-31T06:16:39.639037Z\">%1$s"
						+ "            <q1:result/>%1$s"
						+ "            <q1:verifications>%1$s"
						+ "              <q1:verification name=\"test\">%1$s"
						+ "                <q1:actual/>%1$s"
						+ "                <q1:expected>&lt;?xml version=\"1.0\" encoding=\"utf-8\"?&gt;%1$s"
						+ "&lt;any /&gt;</q1:expected>%1$s"
						+ "                <q1:diff>+ ...&lt;?xml version=\"1.0\" e...</q1:diff>%1$s"
						+ "              </q1:verification>%1$s"
						+ "            </q1:verifications>%1$s"
						+ "          </q1:report>%1$s"
						+ "        </q1:reports>%1$s"
						+ "      </q1:module>%1$s"
						+ "    </q1:step><q1:step name=\"s3\">%1$s"
						+ "      <q1:module>%1$s"
						+ "        <q1:object objectId=\"b\"/>%1$s"
						+ "        <q1:reports>%1$s"
						+ "          <q1:report endTime=\"2012-07-31T06:16:43.676037Z\" startTime=\"2012-07-31T06:16:43.176037Z\">%1$s"
						+ "            <q1:result isException=\"true\">anyException</q1:result>%1$s"
						+ "          </q1:report>%1$s"
						+ "        </q1:reports>%1$s"
						+ "      </q1:module>%1$s"
						+ "    </q1:step></q1:steps></q1:testCase>%1$s"
						+ "</testCases>", System.getProperty("line.separator"));
	}

}
