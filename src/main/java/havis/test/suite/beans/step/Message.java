package havis.test.suite.beans.step;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import havis.test.suite.api.NDIContext;
import havis.test.suite.api.dto.TestCaseInfo;

/**
 * The step property duration provides the duration in milliseconds to sleep.
 */
public class Message implements havis.test.suite.api.Step {
	// private static final Logger log = LoggerFactory.getLogger(Message.class);
	private Map<String, Object> stepProperties;

	/**
	 * See
	 * {@link Havis.RfidTestSuite.Interfaces.Step#prepare(NDIContext, String, TestCaseInfo, String, Map)}
	 */
	@Override
	public Map<String, Object> prepare(NDIContext context, String moduleHome,
			TestCaseInfo testCaseInfo, String stepId,
			Map<String, Object> stepProperties) throws Exception {
		// get Text
		if (!stepProperties.containsKey("text")) {
			throw new ConfigurationException("Step property 'text' is missed");
		}
		this.stepProperties = stepProperties;
		return null;
	}

	/**
	 * See {@link Havis.RfidTestSuite.Interfaces.Step#run()}
	 */
	@Override
	public String run() throws Exception {
		StringBuilder msg = new StringBuilder();
		Object value = stepProperties.get("text");
		if (value instanceof String) {
			msg.append(value);
		} else if (value instanceof List<?>) {
			for (Object v : (ArrayList<?>) value) {
				if (msg.length() > 0) {
					msg.append(System.getProperty("line.separator"));
				}
				msg.append(v);
			}
		}
		if (msg.length() > 0) {
			// add property for UI which calls this method
			stepProperties.put("UI.messageAfterExecute", msg.toString());
		}
		return null;
	}

	/**
	 * See {@link Havis.RfidTestSuite.Interfaces.Step#finish()}
	 */
	@Override
	public void finish() throws Exception {
		// add properties for UI module which calls this method
		if (stepProperties.containsKey("suspendAfterFinish")) {
			stepProperties.put("UI.suspendAfterFinish", Boolean
					.parseBoolean((String) stepProperties
							.get("suspendAfterFinish")));
			if (stepProperties.containsKey("suspendAfterFinishTimeout")) {
				stepProperties.put("UI.suspendAfterFinishTimeout", Integer
						.parseInt((String) stepProperties
								.get("suspendAfterFinishTimeout")));
			}
		}
	}
}
