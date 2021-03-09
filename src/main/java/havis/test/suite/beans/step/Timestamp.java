package havis.test.suite.beans.step;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import havis.test.suite.api.NDIContext;
import havis.test.suite.api.dto.TestCaseInfo;

/**
 * The step property duration provides the duration in milliseconds to sleep.
 */
public class Timestamp implements havis.test.suite.api.Step {
	private static final Logger log = LoggerFactory.getLogger(Timestamp.class);
	private NDIContext context;
	private Map<String, Object> globalContextKey;

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
		if (!stepProperties.containsKey("globalContextKey")) {
			throw new ConfigurationException(
					"Step property 'globalContextKey' is missed");
		}
		globalContextKey = (Map<String, Object>) stepProperties
				.get("globalContextKey");
		if (globalContextKey.size() == 0) {
			throw new ConfigurationException(
					"Step property 'globalContextKey' requires a key as content");
		}
		return null;
	}

	/**
	 * See {@link Havis.RfidTestSuite.Interfaces.Step#run()}
	 */
	@Override
	public String run() throws Exception {
		// create file name and its directory
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		
		for (Entry<String, Object> entry : globalContextKey.entrySet()) {
			Date current = new Date();
			String timestamp = formatter.format(current);
			String community = entry.getKey();
			String path = (String) entry.getValue();
			context.setValue(community, path, timestamp);
			if (log.isInfoEnabled()) {
				log.info("Saved timestamp '" + timestamp
						+ "' using global context key '" + path
						+ "' for community '" + community + "'");
			}
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
