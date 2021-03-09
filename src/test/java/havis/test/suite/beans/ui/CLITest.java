package havis.test.suite.beans.ui;

import havis.test.suite.XMLTypeConverter;
import havis.test.suite.api.UI;
import havis.test.suite.api.dto.TestCaseInfo;
import havis.test.suite.common.PathResolver;
import havis.test.suite.common.helpers.PathResolverFileHelper;
import havis.test.suite.common.ndi.SynchronizedNDIContext;
import havis.test.suite.testcase.EntryType;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test
public class CLITest {
	private Path moduleHome = null;
	private final String creatorBeanFolder = "modules" + File.separator
			+ "Havis.RfidTestSuite.UICommon";
	private final String objectsFile = "beans.xml";
	private GenericApplicationContext objContext;
	private UI ui;
	private UI uiDefaults;
	private final List<String> testCasesDir;

	/**
	 * 
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public CLITest() throws IOException, URISyntaxException {
		List<Path> basePathes = null;
		// Get Paths of classpaths
		basePathes = PathResolverFileHelper.getAbsolutePathFromResource("test");
		if (basePathes != null && basePathes.size() > 0) {
			moduleHome = basePathes.get(0);
		}
		testCasesDir = new ArrayList<String>();
		for (Path testCaseDir : PathResolver.getAbsolutePathFromResource(
				"TestCases", "")) {
			testCasesDir.add(testCaseDir.toString());
		}
	}

	/**
	 * 
	 * @throws IOException
	 */
	@BeforeClass
	public void setUp() throws IOException {
		if (moduleHome != null) {
			// get objects via spring framework
			objContext = new GenericApplicationContext();
			XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(
					objContext);
			Path plPath = Paths.get(moduleHome + File.separator
					+ creatorBeanFolder + File.separator + objectsFile);
			reader.loadBeanDefinitions("file:///" + plPath);
			objContext.refresh();
			ui = (UI) objContext.getBean("ui.cli");
			uiDefaults = (UI) objContext.getBean("ui.cliDefaults");
		}
	}

	@Test
	public void getAppObjectIds() throws Exception {
		// no arguments
		ui.start(new SynchronizedNDIContext(), moduleHome.toString(),
				testCasesDir, new String[0]);
		Assert.assertEquals(ui.getModulesObjectIds().getApps().size(), 0);
		ui.stop();

		uiDefaults.start(new SynchronizedNDIContext(), moduleHome.toString(),
				testCasesDir, new String[0]);
		List<String> apps = uiDefaults.getModulesObjectIds().getApps();
		Assert.assertEquals(apps.size(), 2);
		Assert.assertEquals(apps.get(0), "app1");
		Assert.assertEquals(apps.get(1), "app2");
		uiDefaults.stop();

		// foreign arguments
		try {
			ui.start(new SynchronizedNDIContext(), moduleHome.toString(),
					testCasesDir, new String[] { "-x" });
			Assert.fail();
		} catch (UnrecognizedOptionException e) {
			Assert.assertTrue(e.getMessage().contains("Unrecognized"));
		} catch (Exception e) {
			Assert.fail();
		}

		try {
			uiDefaults.start(new SynchronizedNDIContext(),
					moduleHome.toString(), testCasesDir, new String[] { "-x" });
			Assert.fail();
		} catch (UnrecognizedOptionException e) {
			Assert.assertTrue(e.getMessage().contains("Unrecognized"));
		} catch (Exception e) {
			Assert.fail();
		}

		// empty list
		try {
			ui.start(new SynchronizedNDIContext(), moduleHome.toString(),
					testCasesDir, new String[] { "-a" });
			Assert.fail();
		} catch (MissingArgumentException e) {
			Assert.assertTrue(e.getMessage().contains("Missing"));
		} catch (Exception e) {
			Assert.fail();
		}

		try {
			uiDefaults.start(new SynchronizedNDIContext(),
					moduleHome.toString(), testCasesDir, new String[] { "-a" });
			Assert.fail();
		} catch (MissingArgumentException e) {
			Assert.assertTrue(e.getMessage().contains("Missing"));
		} catch (Exception e) {
			Assert.fail();
		}

		// one argument
		ui.start(new SynchronizedNDIContext(), moduleHome.toString(),
				testCasesDir, new String[] { "-a", "a1" });
		apps = ui.getModulesObjectIds().getApps();
		Assert.assertEquals(apps.size(), 1);
		Assert.assertEquals(apps.get(0), "a1");
		ui.stop();

		uiDefaults.start(new SynchronizedNDIContext(), moduleHome.toString(),
				testCasesDir, new String[] { "-a", "a1" });
		apps = uiDefaults.getModulesObjectIds().getApps();
		Assert.assertEquals(apps.size(), 1);
		Assert.assertEquals(apps.get(0), "a1");
		uiDefaults.stop();

		// two arguments
		ui.start(new SynchronizedNDIContext(), moduleHome.toString(),
				testCasesDir, new String[] { "-a", "a1", "a2" });
		apps = ui.getModulesObjectIds().getApps();
		Assert.assertEquals(apps.size(), 2);
		Assert.assertEquals(apps.get(0), "a1");
		Assert.assertEquals(apps.get(1), "a2");
		ui.stop();

	}

	@Test
	public void getReporterObjectIds() throws Exception {

		// no arguments
		ui.start(new SynchronizedNDIContext(), moduleHome.toString(),
				testCasesDir, new String[0]);
		Assert.assertEquals(ui.getModulesObjectIds().getReporters().size(), 0);
		ui.stop();

		uiDefaults.start(new SynchronizedNDIContext(), moduleHome.toString(),
				testCasesDir, new String[0]);
		List<String> reporters = uiDefaults.getModulesObjectIds()
				.getReporters();
		Assert.assertEquals(reporters.size(), 1);
		Assert.assertEquals(reporters.get(0), "reporter1");
		uiDefaults.stop();

		// foreign arguments
		try {
			ui.start(new SynchronizedNDIContext(), moduleHome.toString(),
					testCasesDir, new String[] { "-x" });
			Assert.fail();
		} catch (UnrecognizedOptionException e) {
			Assert.assertTrue(e.getMessage().contains("Unrecognized"));
		} catch (Exception e) {
			Assert.fail();
		}
		try {
			uiDefaults.start(new SynchronizedNDIContext(),
					moduleHome.toString(), testCasesDir, new String[] { "-x" });
			Assert.fail();
		} catch (UnrecognizedOptionException e) {
			Assert.assertTrue(e.getMessage().contains("Unrecognized"));
		} catch (Exception e) {
			Assert.fail();
		}

		// empty list
		try {
			ui.start(new SynchronizedNDIContext(), moduleHome.toString(),
					testCasesDir, new String[] { "-r" });
			Assert.fail();
		} catch (MissingArgumentException e) {
			Assert.assertTrue(e.getMessage().contains("Missing"));
		} catch (Exception e) {
			Assert.fail();
		}
		try {
			uiDefaults.start(new SynchronizedNDIContext(),
					moduleHome.toString(), testCasesDir, new String[] { "-r" });
			Assert.fail();
		} catch (MissingArgumentException e) {
			Assert.assertTrue(e.getMessage().contains("Missing"));
		} catch (Exception e) {
			Assert.fail();
		}

		// one argument
		ui.start(new SynchronizedNDIContext(), moduleHome.toString(),
				testCasesDir, new String[] { "-r", "r1" });
		reporters = ui.getModulesObjectIds().getReporters();
		Assert.assertEquals(reporters.size(), 1);
		Assert.assertEquals(reporters.get(0), "r1");
		ui.stop();
		uiDefaults.start(new SynchronizedNDIContext(), moduleHome.toString(),
				testCasesDir, new String[] { "-r", "r1" });
		reporters = uiDefaults.getModulesObjectIds().getReporters();
		Assert.assertEquals(reporters.size(), 1);
		Assert.assertEquals(reporters.get(0), "r1");
		uiDefaults.stop();

		// two arguments
		ui.start(new SynchronizedNDIContext(), moduleHome.toString(),
				testCasesDir, new String[] { "-r", "r1", "r2" });
		reporters = ui.getModulesObjectIds().getReporters();
		Assert.assertEquals(reporters.size(), 2);
		Assert.assertEquals(reporters.get(0), "r1");
		Assert.assertEquals(reporters.get(1), "r2");
		ui.stop();
	}

	@Test
	public void getStatisticCreatorObjectIds() throws Exception {

		// no arguments
		ui.start(new SynchronizedNDIContext(), moduleHome.toString(),
				testCasesDir, new String[0]);
		Assert.assertEquals(ui.getModulesObjectIds().getStatisticCreators()
				.size(), 0);
		ui.stop();
		uiDefaults.start(new SynchronizedNDIContext(), moduleHome.toString(),
				testCasesDir, new String[0]);
		List<String> statisticCreators = uiDefaults.getModulesObjectIds()
				.getStatisticCreators();
		Assert.assertEquals(statisticCreators.size(), 1);
		Assert.assertEquals(statisticCreators.get(0), "statisticCreator1");
		uiDefaults.stop();

		// foreign arguments
		try {
			ui.start(new SynchronizedNDIContext(), moduleHome.toString(),
					testCasesDir, new String[] { "-x" });
			Assert.fail();
		} catch (UnrecognizedOptionException e) {
			Assert.assertTrue(e.getMessage().contains("Unrecognized"));
		} catch (Exception e) {
			Assert.fail();
		}
		try {
			uiDefaults.start(new SynchronizedNDIContext(),
					moduleHome.toString(), testCasesDir, new String[] { "-x" });
			Assert.fail();
		} catch (UnrecognizedOptionException e) {
			Assert.assertTrue(e.getMessage().contains("Unrecognized"));
		} catch (Exception e) {
			Assert.fail();
		}

		// empty list
		try {
			ui.start(new SynchronizedNDIContext(), moduleHome.toString(),
					testCasesDir, new String[] { "-s" });
			Assert.fail();
		} catch (MissingArgumentException e) {
			Assert.assertTrue(e.getMessage().contains("Missing"));
		} catch (Exception e) {
			Assert.fail();
		}
		try {
			uiDefaults.start(new SynchronizedNDIContext(),
					moduleHome.toString(), testCasesDir, new String[] { "-s" });
			Assert.fail();
		} catch (MissingArgumentException e) {
			Assert.assertTrue(e.getMessage().contains("Missing"));
		} catch (Exception e) {
			Assert.fail();
		}

		// one argument
		ui.start(new SynchronizedNDIContext(), moduleHome.toString(),
				testCasesDir, new String[] { "-s", "s1" });
		statisticCreators = ui.getModulesObjectIds().getStatisticCreators();
		Assert.assertEquals(statisticCreators.size(), 1);
		Assert.assertEquals(statisticCreators.get(0), "s1");
		ui.stop();
		uiDefaults.start(new SynchronizedNDIContext(), moduleHome.toString(),
				testCasesDir, new String[] { "-s", "s1" });
		statisticCreators = uiDefaults.getModulesObjectIds()
				.getStatisticCreators();
		Assert.assertEquals(statisticCreators.size(), 1);
		Assert.assertEquals(statisticCreators.get(0), "s1");
		uiDefaults.stop();

		// two arguments
		ui.start(new SynchronizedNDIContext(), moduleHome.toString(),
				testCasesDir, new String[] { "-s", "s1", "s2" });
		statisticCreators = ui.getModulesObjectIds().getStatisticCreators();
		Assert.assertEquals(statisticCreators.size(), 2);
		Assert.assertEquals(statisticCreators.get(0), "s1");
		Assert.assertEquals(statisticCreators.get(1), "s2");
		ui.stop();
	}

	@Test
	public void getTestCase() throws Exception {
		// no arguments
		ui.start(new SynchronizedNDIContext(), moduleHome.toString(),
				testCasesDir, new String[0]);
		Assert.assertEquals(ui.getModulesObjectIds().getStatisticCreators()
				.size(), 0);
		ui.stop();
		// foreign arguments
		try {
			ui.start(new SynchronizedNDIContext(), moduleHome.toString(),
					testCasesDir, new String[] { "-x" });
			Assert.fail();
		} catch (UnrecognizedOptionException e) {
			Assert.assertTrue(e.getMessage().contains("Unrecognized"));
		} catch (Exception e) {
			Assert.fail();
		}
		// test case argument
		ui.start(new SynchronizedNDIContext(), moduleHome.toString(),
				testCasesDir, new String[] { "-t", "t0", "t1" });
		List<String> testCasesNames = ui.getTestCasesPaths();

		for (int i = 0; i < 2; i++) {
			Assert.assertEquals(testCasesNames.size(), 1);
			Assert.assertEquals(testCasesNames.get(0), "t" + i);
			testCasesNames = ui.getTestCasesPaths();
		}
		Assert.assertEquals(testCasesNames.size(), 0);
		ui.stop();
	}

	@Test
	public void count() throws Exception {
		// infinite loop (CLI parameter)
		ui.start(new SynchronizedNDIContext(), moduleHome.toString(),
				testCasesDir, new String[] { "-c", "-1", "-t", "a1" });
		List<String> testCaseNames = ui.getTestCasesPaths();

		for (int i = 0; i < 10; i++) {
			Assert.assertEquals(testCaseNames.size(), 1);
			Assert.assertEquals(testCaseNames.get(0), "a1");
			testCaseNames = ui.getTestCasesPaths();
		}
		ui.stop();

		// count = 0 (CLI parameter)
		ui.start(new SynchronizedNDIContext(), moduleHome.toString(),
				testCasesDir, new String[] { "-c", "0", "-t", "a1" });
		Assert.assertEquals(ui.getTestCasesPaths().size(), 0);
		ui.stop();

		// default behaviour without config entry or CLI parameter: count = 1
		ui.start(new SynchronizedNDIContext(), moduleHome.toString(),
				testCasesDir, new String[] { "-t", "a1" });
		testCaseNames = ui.getTestCasesPaths();
		Assert.assertEquals(testCaseNames.size(), 1);
		Assert.assertEquals(testCaseNames.get(0), "a1");
		Assert.assertEquals(ui.getTestCasesPaths().size(), 0);
		ui.stop();

		// count = 2 using CLI parameter
		ui.start(new SynchronizedNDIContext(), moduleHome.toString(),
				testCasesDir, new String[] { "-c", "2", "-t", "a1" });
		Assert.assertEquals(ui.getTestCasesPaths().size(), 1);
		Assert.assertEquals(ui.getTestCasesPaths().size(), 1);
		Assert.assertEquals(ui.getTestCasesPaths().size(), 0);
		ui.stop();

		// count = 2 using config entry
		uiDefaults.start(new SynchronizedNDIContext(), moduleHome.toString(),
				testCasesDir, new String[] { "-t", "a0", "a1", "a2" });
		testCaseNames = uiDefaults.getTestCasesPaths();
		for (int i = 0; i < 2; i++) {
			for (int y = 0; y < 3; y++) {
				Assert.assertEquals(testCaseNames.size(), 1);
				Assert.assertEquals(testCaseNames.get(0), "a" + y);
				testCaseNames = uiDefaults.getTestCasesPaths();
			}
		}
		Assert.assertEquals(testCaseNames.size(), 0);
		uiDefaults.stop();

		// invalid count value
		try {
			ui.start(new SynchronizedNDIContext(), moduleHome.toString(),
					testCasesDir, new String[] { "-c", "a", "-t", "a1" });
			Assert.fail();
		} catch (ArgumentException e) {
			Assert.assertTrue(e.getMessage().contains("'c'"));
		} catch (Exception e) {
			Assert.fail();
		}
	}

	@Test
	public void executeStep() throws Exception {
		Step step = new Step();
		List<EntryType> stepProperties = new ArrayList<EntryType>();
		Map<String, Object> stepProps = new XMLTypeConverter().convert(
				stepProperties, null);
		TestCaseInfo testCaseInfo = new TestCaseInfo();
		testCaseInfo.setName("testCaseName");
		testCaseInfo.setHome("testCaseHome");
		testCaseInfo.setId("testCaseId");
		ui.prepareExecute(step, testCaseInfo, "moduleHome", "stepId", stepProps);
		Assert.assertEquals(step.getModuleHome(), "moduleHome");
		Assert.assertEquals(step.getStepProperties().values(), stepProperties);
		Assert.assertEquals(step.getTestCaseInfo().getName(), "testCaseName");
		Assert.assertEquals(step.getTestCaseInfo().getHome(), "testCaseHome");
		Assert.assertEquals(step.getTestCaseInfo().getId(), "testCaseId");
		Assert.assertEquals(step.getStepId(), "stepId");
		ui.execute(step, stepProps);
		Assert.assertTrue(step.isRunCalled());
		ui.finishExec(step, stepProps, null);
		Assert.assertTrue(step.isFinishedCalled());
	}

}
