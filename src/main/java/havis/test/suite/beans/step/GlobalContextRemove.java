package havis.test.suite.beans.step;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import havis.test.suite.api.NDIContext;
import havis.test.suite.api.dto.TestCaseInfo;

/**
 * The step property duration provides the duration in milliseconds to sleep.
 */
public class GlobalContextRemove implements havis.test.suite.api.Step {
	private static final Logger log = LoggerFactory
			.getLogger(GlobalContextRemove.class);
	private Map<String, Object> keys;
	private NDIContext context;

	/**
	 * See
	 * {@link Havis.RfidTestSuite.Interfaces.Step#prepare(NDIContext, String, TestCaseInfo, String, Map)}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> prepare(NDIContext context, String moduleHome,
			TestCaseInfo testCaseInfo, String stepId,
			Map<String, Object> stepProperties) throws Exception {
		this.context = context;
		// get keys
		if (!stepProperties.containsKey("keys")) {
			throw new ConfigurationException("Step property 'keys' is missed");
		}
		keys = (Map<String, Object>) stepProperties.get("keys");
		return null;
	}

	/**
	 * See {@link Havis.RfidTestSuite.Interfaces.Step#run()}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public String run() throws Exception {
		int count = 0;
		for (Entry<String, Object> entry : keys.entrySet()) {
			List<Object> paths = new ArrayList<Object>();
			if (entry.getValue() instanceof String) {
				paths.add(entry.getValue());
			} else {
				paths = (List<Object>) entry.getValue();
			}
			for (Object path : paths) {
				String community = entry.getKey();
				context.removeValue(community, (String) path);
				if (log.isDebugEnabled()) {
					log.debug("Removing context key '" + path
							+ "' from community '" + community + "'");
				}
			}
			count += paths.size();
		}

		if (log.isInfoEnabled()) {
			log.info("Removed " + count + " context key"
					+ (count != 1 ? "s" : ""));
		}

		return null;
	}

	/**
	 * See {@link Havis.RfidTestSuite.Interfaces.Step#finish()}
	 */
	@Override
	public void finish() throws Exception {
	}

}
