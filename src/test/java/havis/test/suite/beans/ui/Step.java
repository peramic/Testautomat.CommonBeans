package havis.test.suite.beans.ui;

import java.util.Map;

import havis.test.suite.api.NDIContext;
import havis.test.suite.api.dto.TestCaseInfo;


public class Step implements havis.test.suite.api.Step {

	private boolean throwRunException;
	private boolean throwFinishException;
	
	private String moduleHome;
	private TestCaseInfo testCaseInfo;
	private String stepId;
	private Map<String, Object> stepProperties;
	
	private boolean isRunCalled;
	private boolean isFinishedCalled;
	
	public boolean isThrowRunException() {
		return throwRunException;
	}

	public void setThrowRunException(boolean throwRunException) {
		this.throwRunException = throwRunException;
	}

	public boolean isThrowFinishException() {
		return throwFinishException;
	}

	public void setThrowFinishException(boolean throwFinishException) {
		this.throwFinishException = throwFinishException;
	}

	public String getModuleHome() {
		return moduleHome;
	}

	public void setModuleHome(String moduleHome) {
		this.moduleHome = moduleHome;
	}

	public TestCaseInfo getTestCaseInfo() {
		return testCaseInfo;
	}

	public void setTestCaseInfo(TestCaseInfo testCaseInfo) {
		this.testCaseInfo = testCaseInfo;
	}

	public String getStepId() {
		return stepId;
	}

	public void setStepId(String stepId) {
		this.stepId = stepId;
	}

	public Map<String, Object> getStepProperties() {
		return stepProperties;
	}

	public void setStepProperties(Map<String, Object> stepProperties) {
		this.stepProperties = stepProperties;
	}

	public boolean isRunCalled() {
		return isRunCalled;
	}

	public void setRunCalled(boolean isRunCalled) {
		this.isRunCalled = isRunCalled;
	}

	public boolean isFinishedCalled() {
		return isFinishedCalled;
	}

	public void setFinishedCalled(boolean isFinishedCalled) {
		this.isFinishedCalled = isFinishedCalled;
	}

	public void initialize()
	{
		throwRunException = false;
		isRunCalled = false;
		throwFinishException = false;
		isFinishedCalled = false;
	}
	

	@Override
	public Map<String, Object> prepare(NDIContext context, String moduleHome,
			TestCaseInfo testCaseInfo, String stepId,
			Map<String, Object> stepProperties) {
		this.moduleHome = moduleHome;
		this.testCaseInfo = testCaseInfo;
		this.stepId = stepId;
		this.stepProperties = stepProperties;
		// return input properties as source properties
		return stepProperties;
	}

	@Override
	public String run() throws Exception {
		isRunCalled = true;
		if (throwRunException)
		{
			throw new Exception("run failed");
		}
		return "<anyResult />";
	}

	@Override
	public void finish() throws Exception {
		isFinishedCalled = true;
		if(throwFinishException)
		{
			throw new Exception("finish failed");
		}
		
	}

}
