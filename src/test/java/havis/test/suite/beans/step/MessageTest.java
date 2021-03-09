package havis.test.suite.beans.step;

import havis.test.suite.api.Step;
import havis.test.suite.common.helpers.PathResolverFileHelper;
import havis.test.suite.common.ndi.SynchronizedNDIContext;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class MessageTest {

	private final String moduleHome;
	private final String objectsFile = "beans.xml";
	private GenericApplicationContext objContext;

	public MessageTest() throws IOException, URISyntaxException {
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
		Step msg = (Step) objContext.getBean("stepsCommon.message");
		// message with a single line
		Map<String, Object> stepProperties = new HashMap<String, Object>();
		stepProperties.put("text", "a");
		msg.prepare(new SynchronizedNDIContext(), moduleHome, null, "stepId",
				stepProperties);
		msg.run();
		msg.finish();
		Assert.assertEquals(stepProperties.get("UI.messageAfterExecute"), "a");
		Assert.assertFalse(stepProperties.containsKey("UI.suspendAfterFinish"));
		Assert.assertFalse(stepProperties
				.containsKey("UI.suspendAfterFinishTimeout"));

		// message with multiple lines
		// and existing "suspendAfterFinish" + "suspendAfterFinishTimeout"
		// properties
		stepProperties.put("text", "a" + System.getProperty("line.spearator")
				+ "b");
		stepProperties.put("suspendAfterFinish", "true");
		stepProperties.put("suspendAfterFinishTimeout", "3");
		msg.prepare(new SynchronizedNDIContext(), moduleHome, null, "stepId",
				stepProperties);
		msg.run();
		msg.finish();
		Assert.assertEquals(stepProperties.get("UI.messageAfterExecute"), "a"
				+ System.getProperty("line.spearator") + "b");

		Assert.assertTrue((boolean) stepProperties.get("UI.suspendAfterFinish"));
		Assert.assertEquals(
				(int) stepProperties.get("UI.suspendAfterFinishTimeout"), 3);

		// message with multiple lines
		// and existing "suspendAfterFinish" property ("false")
		stepProperties.put("suspendAfterFinish", "false");
		msg.prepare(new SynchronizedNDIContext(), moduleHome, null, "stepId",
				stepProperties);
		msg.run();
		msg.finish();
		Assert.assertFalse((boolean) stepProperties
				.get("UI.suspendAfterFinish"));

		try {
			// invalid step property name
			stepProperties = new HashMap<String, Object>();
			stepProperties.put("a", "1000");
			msg.prepare(new SynchronizedNDIContext(), moduleHome, null,
					"stepId", stepProperties);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().contains("is missed"));
			msg.finish();
		}

		try {
			// no step properties
			stepProperties = new HashMap<String, Object>();
			msg.prepare(new SynchronizedNDIContext(), moduleHome, null,
					"stepId", stepProperties);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().contains("is missed"));
			msg.finish();
		}

	}
}
