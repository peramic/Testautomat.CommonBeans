package havis.test.suite.beans.step;

import havis.test.suite.api.Step;
import havis.test.suite.common.helpers.PathResolverFileHelper;
import havis.test.suite.common.ndi.SynchronizedNDIContext;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SleepTest {

	private final String moduleHome;
	private final String objectsFile = "beans.xml";
	private GenericApplicationContext objContext;

	public SleepTest() throws IOException, URISyntaxException {
		moduleHome = PathResolverFileHelper
				.getAbsolutePathFromResource(
						"test/modules/Havis.RfidTestSuite.StepsCommon").get(0)
				.toString();
	}

	@BeforeClass
	public void setUp() {
		// get objects via spring framework
		objContext = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(objContext);
		reader.loadBeanDefinitions("file:///" + moduleHome + "/" + objectsFile);
		objContext.refresh();
	}

	@Test
	public void run() throws Exception {
		Step sleep = (Step) objContext.getBean("stepsCommon.sleep");

		// duration 1000 ms
		Map<String, Object> dict = new HashMap<String, Object>();
		dict.put("duration", "1000");
		sleep.prepare(new SynchronizedNDIContext(), moduleHome, null, "stepId",
				dict);
		Date startTime = new Date();
		sleep.run();
		sleep.finish();
		Date endTime = new Date();
		long duration = endTime.getTime() - startTime.getTime();
		Assert.assertTrue(duration > 800);
		Assert.assertTrue(duration < 1200);

		try {
			// invalid step property name
			dict = new HashMap<String, Object>();
			dict.put("a", "1000");
			sleep.prepare(new SynchronizedNDIContext(), moduleHome, null,
					"stepId", dict);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().contains("is missed"));
			sleep.finish();
		}

		try {
			// no step property
			dict = new HashMap<String, Object>();
			sleep.prepare(new SynchronizedNDIContext(), moduleHome, null,
					"stepId", dict);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().contains("is missed"));
			sleep.finish();
		}
	}
}
