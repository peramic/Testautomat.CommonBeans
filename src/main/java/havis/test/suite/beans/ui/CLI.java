package havis.test.suite.beans.ui;

import havis.test.suite.api.NDIContext;
import havis.test.suite.api.Step;
import havis.test.suite.api.UI;
import havis.test.suite.api.dto.ModulesObjectIds;
import havis.test.suite.api.dto.TestCaseInfo;
import havis.test.suite.api.dto.VerificationReport;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * A UI using the command line (see "Help" for options). In the deployment
 * descriptor object.xml the property {@link #def} provides the default values
 * (see "def").
 */

public class CLI implements UI {
	private static final Logger log = LoggerFactory.getLogger(CLI.class);

	private class WebHTTPServiceHost {
		private HttpServer host;
		private List<String> context = new ArrayList<>();
		private int port;
		private String uri;
		private CountDownLatch call = new CountDownLatch(1);
		private long timeout = 60000;

		public void awaitCall() throws InterruptedException {
			call.await(timeout, TimeUnit.MILLISECONDS);			
		}

		public WebHTTPServiceHost(int port) throws IOException,
				InterruptedException {
			host = HttpServer.create(new InetSocketAddress(port), 0);
			host.start();
			this.port = port;
		}
		
		public void setTimeout(long timeout) {
			this.timeout = timeout > 0 ? timeout : 60000;
		}

		public void open(String path) throws InterruptedException {
			uri = "0.0.0.0" + ":" + port + path;
			log.info("Opening click listener at " + uri);
			host.createContext(path, new HttpHandler() {
				@Override
				public void handle(HttpExchange exchange) throws IOException {
					exchange.sendResponseHeaders(200, -1);
					call.countDown();
				}
			});
			context.add(path);
		}

		public void close(String path) throws URISyntaxException {
			log.info("Closing service at " + uri);
			if (host != null) {
				host.removeContext(path);
				host.stop(0);
				host = null;
			}
			log.info("done");
		}

	}

	private NDIContext context;
	CommandLine cli;

	private final Object lockObject = new Object();
	private int remainingExecCount;
	private List<String> testCasesPaths;
	private int lastTestCasesPathsIndex;
	private boolean remoteAccess;
	
	private String baseURI;

	public boolean isRemoteAccess() {
		return remoteAccess;
	}

	public void setRemoteAccess(boolean remoteAccess) {
		this.remoteAccess = remoteAccess;
	}

	public String getBaseURI() {
		return baseURI;
	}

	public void setBaseURI(String baseURI) {
		this.baseURI = baseURI;
	}

	/**
	 * Default values from property def in deployment descriptor: <code>
	 * dictionary
	 * 		entry key "appObjectIds" / "reporterObjectIds" / 
	 * 			"statisticCreatorObjectIds" / "testCasesObjectIds"
	 * 		  list
	 * 			value
	 * 			value
	 * 			...	
	 * 		entry key "count"
	 * </code>
	 */
	private Map<?, ?> def;

	public Map<?, ?> getDef() {
		return def;
	}

	public void setDef(Map<?, ?> def) {
		this.def = def;
	}

	/**
	 * See
	 * {@link havis.test.suite.api.UI#start(NDIContext, String, List, String[])}
	 */
	@Override
	public void start(NDIContext context, String moduleHome,
			List<String> testCasesHome, String[] cliArgs)
			throws ParseException, ArgumentException {
		this.context = context;

		// Create a BasicParser to build up cli options (apache cli)
		CommandLineParser parser = new DefaultParser();
		Options options = new Options();		

		// Add Options
		options.addOption(Option
				.builder("c")
				.hasArgs()
				.argName("count")
				.desc("count of test case executions "
						+ System.getProperty("line.separator")
						+ " infinite loop: -1"
						+ System.getProperty("line.separator")
						+ " The execution can be cancelled with CTRL-C")
				.build());
		options.addOption(Option.builder("h").argName("help").desc("This Help")
				.build());
		options.addOption(Option.builder("t").hasArgs()
				.desc("Paths to the test casess").argName("path").build());
		options.addOption(Option.builder("a").hasArgs()
				.desc("objectIds of application modules").argName("objectIds")
				.build());
		options.addOption(Option.builder("r").hasArgs()
				.desc("objectIds of reporter modules").argName("objectIds")
				.build());
		options.addOption(Option.builder("s").hasArgs()
				.desc("objectIds of statistic creator modules")
				.argName("objectIds").build());

		// Parse Arguments
		cli = parser.parse(options, cliArgs);
		// Help Menu
		if (cli.hasOption("h")) {
			HelpFormatter formater = new HelpFormatter();
			formater.printHelp("Testautomat", options);
			System.out
					.println("The default values can be set in the configuration file of the"
							+ System.getProperty("line.separator")
							+ "user interface module (beans.xml)");
			return;
		}
		// get remaining exec count
		// (CLI options has higher priority than default values in config)
		remainingExecCount = 1;
		if (def != null && def.containsKey("count")) {
			try {
				// use exec count from config
				remainingExecCount = Integer
						.parseInt((String) def.get("count"));
			} catch (Exception e) {
				throw new ArgumentException(
						"Type of configuration property 'count' must be 'integer'",
						e);
			}
		}
		if (cli.hasOption("c")) {
			try {
				remainingExecCount = Integer.parseInt(cli.getOptionValue("c"));
			} catch (Exception e) {
				throw new ArgumentException(
						"Type of CLI option 'c' must be 'integer'", e);
			}
		}

		// get paths to test cases
		testCasesPaths = getOptionList("t", "testCasesPaths");
		lastTestCasesPathsIndex = -1;

		// TODO [POSTPONED]Cancel event
		Runtime rt = Runtime.getRuntime();
		rt.addShutdownHook(new Thread() {
			public void run() {
				log.warn("--------------------------------------------------------");
				log.warn("The processing of the test cases has been ended.");
				log.warn("--------------------------------------------------------");
				// reset remaining execution count
				synchronized (lockObject) {
					remainingExecCount = 0;
				}
			};
		});
	}

	/**
	 * See {@link havis.test.suite.api.UI#stop()}
	 */
	@Override
	public void stop() {
		context = null;
		cli = null;
	}

	/**
	 * See {@link havis.test.suite.api.UI#getModulesObjectIds()}
	 */
	@Override
	public ModulesObjectIds getModulesObjectIds() {
		ModulesObjectIds objs = new ModulesObjectIds();
		objs.setApps(getOptionList("a", "appsObjectIds"));
		objs.setReporters(getOptionList("r", "reportersObjectIds"));
		objs.setStatisticCreators(getOptionList("s",
				"statisticCreatorsObjectIds"));
		return objs;
	}

	/**
	 * See {@link havis.test.suite.api.UI#getTestCasesPaths()}
	 */
	@Override
	public List<String> getTestCasesPaths() {
		List<String> ret = new ArrayList<String>();
		// protect access to "remainingExecCount"
		synchronized (lockObject) {
			// if a test case exists and shall be executed
			if (remainingExecCount != 0 && testCasesPaths.size() > 0) {
				// increase list index
				lastTestCasesPathsIndex++;
				// if it was the last test case in list
				if (lastTestCasesPathsIndex == testCasesPaths.size()) {
					// reset list index to first test case
					lastTestCasesPathsIndex = 0;
					// if not infinite loop
					if (remainingExecCount > 0) {
						// decrease exec count
						remainingExecCount--;
					}
				}
				if (remainingExecCount != 0) {
					// return next test case
					ret.add(testCasesPaths.get(lastTestCasesPathsIndex));
				}
			}
		}
		return ret;
	}

	/**
	 * See
	 * {@link havis.test.suite.api.UI#prepareExecute(Step, TestCaseInfo, String, String, Map)}
	 * 
	 * @throws Exception
	 */
	@Override
	public Map<String, Object> prepareExecute(Step step,
			TestCaseInfo testCaseInfo, String moduleHome, String stepId,
			Map<String, Object> stepProperties) throws Exception {
		return step.prepare(context, moduleHome, testCaseInfo, stepId,
				stepProperties);
	}

	/**
	 * See {@link havis.test.suite.api.UI#execute(Step, Map)}
	 */
	@Override
	public String execute(Step step, Map<String, Object> stepProperties)
			throws Exception {
		if (stepProperties != null && stepProperties.containsKey("UI.messageBeforeExecute")) {
			Object msg = stepProperties.get("UI.messageBeforeExecute");
			for (String line : msg.toString().split(System.getProperty("line.separator"))) {
				log.info("<-- " + line);
			}
		}
		String ret = step.run();
		if (stepProperties != null
				&& stepProperties.containsKey("UI.messageAfterExecute")) {
			Object msg = stepProperties.get("UI.messageAfterExecute");
			for (String line : msg.toString().split(
					System.getProperty("line.separator"))) {
				log.info("<-- " + line);
			}
		}
		return ret;
	}

	/**
	 * See {@link havis.test.suite.api.UI#finishExec(Step, Map, List)}
	 */
	@Override
	public void finishExec(Step step, Map<String, Object> stepProperties,
			List<VerificationReport> verificationReports) throws Exception {
		step.finish();
		if (stepProperties != null
				&& stepProperties.containsKey("UI.suspendAfterFinish")) {
			if ((boolean) stepProperties.get("UI.suspendAfterFinish")) {
				if (stepProperties.containsKey("UI.suspendAfterFinishTimeout")) {
					int timeout = (int) stepProperties
							.get("UI.suspendAfterFinishTimeout");
					long millis = System.currentTimeMillis();
					for (int i = timeout; i > 0; i--) {
						log.info("<-- " + i + " second" + (i > 1 ? "s" : ""));
						try {
							Thread.sleep((int) (1000 - (System
									.currentTimeMillis() - millis)));
						} catch (InterruptedException e) {
							log.error("", e);
						}
						millis = System.currentTimeMillis();
					}

				} else {

					if (remoteAccess) {
						URI uri = new URI(baseURI);
						int port = uri.getPort() + 1;
						WebHTTPServiceHost host = new WebHTTPServiceHost(port);
						if (stepProperties.containsKey(NDIConstants.getUIRemoteTimeout())) {
							long timeout = Long.valueOf(stepProperties.get(NDIConstants.getUIRemoteTimeout()) + "");
							host.setTimeout(timeout);
						} else {
							Object timeoutValue = context.getValue(NDIConstants.getUIcummunity(), havis.test.suite.beans.ui.NDIConstants.getUIRemoteTimeout());
							if(timeoutValue != null && !timeoutValue.toString().trim().isEmpty()) {
								long timeout = Long.valueOf(timeoutValue.toString());
								host.setTimeout(timeout);
							}							
						}
						String randomContext = "/"
								+ UUID.randomUUID().toString();
						host.open(randomContext);
						log.info("Click http://"+uri.getHost()+":"+port+randomContext+" to continue (" + host.timeout + ")");
						//log.info("Click " + "http://"+uri.getHost()+":"+port+randomContext);
						host.awaitCall();
						host.close(randomContext);
					} else {
						log.info("<-- Please press <enter> to continue...");
						System.in.read(new byte[2]);
					}

				}
			}
		}

	}

	/**
	 * Gets a list of values for an command line option. If no value is provided
	 * via command line the default values from the module configuration are
	 * returned.
	 * 
	 * @param option
	 * @param defaultKey
	 *            configuration key for the default values
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<String> getOptionList(String option, String defaultKey) {
		List<String> ret = new ArrayList<String>();

		if (cli.hasOption(option)) {
			ret.addAll(Arrays.asList(cli.getOptionValues(option)));
		} else if (def != null && def.containsKey(defaultKey)) {
			// add default values
			for (String v : (List<String>) def.get(defaultKey)) {
				ret.add(v);
			}
		}
		return ret;
	}

}
