package havis.test.suite.beans.reporter;

import havis.test.suite.api.Reporter;
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

public class FilesTest {
	private Path moduleHome = null;
	private final String reporterBeanFolder = "modules" + File.separator
			+ "Havis.RfidTestSuite.ReportersCommon";
	private final String objectsFile = "beans.xml";
	// Output directory in clathpath (Point Seperated relative to classpath)
	private String outputDir = "output";
	private GenericApplicationContext objContext;

	/**
	 * 
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public FilesTest() throws IOException, URISyntaxException {
		List<Path> basePathes = null;
		// Get Paths of classpaths
		basePathes = PathResolverFileHelper.getAbsolutePathFromResource("test");
		if (basePathes != null && basePathes.size() > 0) {
			moduleHome = basePathes.get(0);
		}
		basePathes = PathResolverFileHelper
				.getAbsolutePathFromResource(outputDir);
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
					+ reporterBeanFolder + File.separator + objectsFile);

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
	public void report() throws Exception {

		// start reporter and send intermediate report (no endTime attribute)
		// expected: a report file with valid file name and content
		Reporter reporter = (Reporter) objContext
				.getBean("reportersCommon.files");
		reporter.start(new SynchronizedNDIContext(), moduleHome.toString(),
				outputDir);
		String start = DateHelper.getDateString(new Date())
				+ "_testCase1_intermediate.xml";
		reporter.report("<testCase><report name=\"testCase1\" /></testCase>");
		String end = DateHelper.getDateString(new Date())
				+ "_testCase1_intermediate.xml";
		reporter.stop();
		File list = new File(outputDir);

		Assert.assertEquals(list.list().length, 1);
		String file = list.list()[0];
		Assert.assertTrue(file.endsWith("_intermediate.xml"));
		Assert.assertTrue(start.compareTo(file) <= 0);
		Assert.assertTrue(file.compareTo(end) <= 0);
		String content = FileHelper.readFile(outputDir + File.separator + file);
		Assert.assertEquals(content,
				"<testCase><report name=\"testCase1\" /></testCase>");
		FileHelper.deleteFiles(outputDir);

		// send full report
		// expected: report files with valid file name and content
		reporter = (Reporter) objContext.getBean("reportersCommon.files");
		reporter.start(new SynchronizedNDIContext(), moduleHome.toString(),
				outputDir);
		start = DateHelper.getDateString(new Date()) + "_testCase1.xml";
		reporter.report("<testCase><report name=\"testCase1\" endTime=\"2012-09-13T07:57:47.4642136+02:00\" /></testCase>");
		end = DateHelper.getDateString(new Date()) + "_testCase1.xml";
		reporter.stop();
		list = new File(outputDir);
		Assert.assertEquals(list.list().length, 1);
		Arrays.sort(list.list());
		file = list.list()[0];
		Assert.assertTrue(file.endsWith("_testCase1.xml"));
		Assert.assertTrue(start.compareTo(file) <= 0);
		Assert.assertTrue(file.compareTo(end) <= 0);
		content = FileHelper.readFile(outputDir + File.separator + file);
		Assert.assertEquals(
				content,
				"<testCase><report name=\"testCase1\" endTime=\"2012-09-13T07:57:47.4642136+02:00\" /></testCase>");
		FileHelper.deleteFiles(outputDir);

	}

	/**
	 * 
	 * @throws Exception
	 */
	@Test
	public void cleanup() throws Exception {
		Reporter reporter = (Reporter) objContext
				.getBean("reporters.files.fileLastWriteTimeInterval");
		reporter.start(new SynchronizedNDIContext(), moduleHome.toString(),
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
		reporter.cleanup();
		reporter.stop();
		File files = new File(outputDir);
		String[] l = files.list();
		Arrays.sort(l);
		Assert.assertEquals(l.length, 4);
		Assert.assertTrue(l[0].endsWith("1.xml"));
		Assert.assertTrue(l[1].endsWith("2.xml"));
		Assert.assertTrue(l[2].endsWith("3.xml"));
		Assert.assertTrue(l[3].endsWith("x.txt"));

		// delete oldest file "1.xml" using max. count = 2
		reporter = (havis.test.suite.beans.reporter.Files) objContext
				.getBean("reporters.files.fileCount");
		reporter.start(new SynchronizedNDIContext(), moduleHome.toString(),
				outputDir);
		reporter.cleanup();
		reporter.stop();
		files = new File(outputDir);
		l = files.list();
		Arrays.sort(l);
		Assert.assertEquals(l.length, 3);
		Assert.assertTrue(l[0].endsWith("2.xml"));
		Assert.assertTrue(l[1].endsWith("3.xml"));
		Assert.assertTrue(l[2].endsWith("x.txt"));

		// delete oldest file "2.xml" using max size = 2 kB ("3.xml": 1100
		// bytes)
		reporter = (havis.test.suite.beans.reporter.Files) objContext
				.getBean("reporters.files.directorySize");
		reporter.start(new SynchronizedNDIContext(), moduleHome.toString(),
				outputDir);
		reporter.cleanup();
		reporter.stop();
		files = new File(outputDir);
		l = files.list();
		Arrays.sort(l);
		Assert.assertEquals(l.length, 2);
		Assert.assertTrue(l[0].endsWith("3.xml"));
		Assert.assertTrue(l[1].endsWith("x.txt"));

		FileHelper.deleteFiles(outputDir);
	}
}
