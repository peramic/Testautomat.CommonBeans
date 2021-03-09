package havis.test.suite.beans.step;

import havis.test.suite.api.NDIContext;
import havis.test.suite.api.NDIProvider;
import havis.test.suite.api.Step;
import havis.test.suite.common.helpers.PathResolverFileHelper;
import havis.test.suite.common.ndi.MapNDIProvider;
import havis.test.suite.common.ndi.SynchronizedNDIContext;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ContextRemoveTest {

	private final String moduleHome;
	private final String objectsFile = "beans.xml";
	private GenericApplicationContext objContext;

	public ContextRemoveTest() throws IOException, URISyntaxException {
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
		Step contextRemove = (Step) objContext
				.getBean("stepsCommon.globalContextRemove");
		Map<String, Object> stepProperties = new HashMap<String, Object>();

		try {
			// invalid step property name
			stepProperties.put("a", "1000");
			contextRemove.prepare(new SynchronizedNDIContext(), moduleHome,
					null, "stepId", stepProperties);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().contains("is missed"));
			contextRemove.finish();
		}

		// remove existing keys
		NDIContext context = new SynchronizedNDIContext();
		NDIProvider prov = new MapNDIProvider();
		context.setProvider(prov);
		context.setValue("community1", "/a", "1");
		context.setValue("community1", "/b", "2");
		context.setValue("community2", "/a", "3");
		context.setValue("community2", "/b", "4");

		stepProperties = new HashMap<String, Object>();
		Map<String, Object> values = new HashMap<String, Object>();
		List<String> list = new ArrayList<String>();
		list.add("/b");
		values.put("community1", "/a");
		values.put("community2", list);
		stepProperties.put("keys", values);
		contextRemove.prepare(context, moduleHome, null, "stepId",
				stepProperties);
		contextRemove.run();
		contextRemove.finish();

		Assert.assertNull(context.getValue("community1", "/a"));
		Assert.assertEquals(context.getValue("community2", "/a"), "3");
		Assert.assertNull(context.getValue("community2", "/b"));
		Assert.assertEquals(context.getValue("community1", "/b"), "2");

		// remove a non-existing key
		stepProperties = new HashMap<String, Object>();
		values = new HashMap<String, Object>();
		values.put("community", "/b");
		stepProperties.put("keys", values);
		contextRemove.run();
		contextRemove.finish();

	}
}
