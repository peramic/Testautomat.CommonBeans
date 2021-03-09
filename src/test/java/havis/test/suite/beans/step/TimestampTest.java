package havis.test.suite.beans.step;

import havis.test.suite.api.NDIContext;
import havis.test.suite.api.Step;
import havis.test.suite.common.helpers.PathResolverFileHelper;
import havis.test.suite.common.ndi.MapNDIProvider;
import havis.test.suite.common.ndi.SynchronizedNDIContext;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TimestampTest {

	private final String moduleHome;
	private final String objectsFile = "beans.xml";
	private GenericApplicationContext objContext;

	public TimestampTest() throws IOException, URISyntaxException {
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
		Step timestamp = (Step) objContext.getBean("stepsCommon.timestamp");
		Map<String, Object> stepProperties = new HashMap<String, Object>();

		try {
			// invalid step property name
			stepProperties.put("a", "1000");
			timestamp.prepare(new SynchronizedNDIContext(), moduleHome, null,
					"stepId", stepProperties);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().contains("is missed"));
			timestamp.finish();
		}

		try {
			// invalid step property name
			stepProperties = new HashMap<String, Object>();
			stepProperties.put("globalContextKey",
					new HashMap<String, Object>());
			timestamp.prepare(new SynchronizedNDIContext(), moduleHome, null,
					"stepId", stepProperties);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(e.getMessage().contains("requires"));
			timestamp.finish();
		}

		NDIContext context = new SynchronizedNDIContext();
		MapNDIProvider ndiProv = new MapNDIProvider();
		context.setProvider(ndiProv);

		stepProperties = new HashMap<String, Object>();
		Map<String, Object> dict = new HashMap<String, Object>();
		dict.put("community1", "/a");
		Date start = new Date();
		stepProperties.put("globalContextKey", dict);
		timestamp.prepare(context, moduleHome, null, "stepId", stepProperties);
		timestamp.run();
		timestamp.finish();
		Date stop = new Date();
		String timestampStr = (String) context.getValue("community1", "/a");

		SimpleDateFormat formatter = new SimpleDateFormat(
				"yyyy-MM-dd'T'HH-mm-ss.SSS");
		Date dateStamp = formatter.parse(timestampStr);
		Assert.assertTrue(start.compareTo(dateStamp) <= 0
				&& dateStamp.compareTo(stop) <= 0);
	}
}
