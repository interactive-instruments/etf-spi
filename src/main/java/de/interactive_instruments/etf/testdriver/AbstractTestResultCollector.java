/*
 * Copyright ${year} interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.interactive_instruments.etf.testdriver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public abstract class AbstractTestResultCollector implements TestResultCollector {

	protected final static Logger errorLogger = LoggerFactory.getLogger(TestResultCollector.class);

	protected enum ResultListenerState {
		READY,

		WRITING_TEST_TASK_RESULT,

		WRITING_TEST_MODULE_RESULT,

		WRITING_TEST_CASE_RESULT,

		WRITING_TEST_STEP_RESULT,

		WRITING_TEST_ASSERTION_RESULT,

		TEST_ASSERTION_RESULT_FINISHED,

		TEST_STEP_RESULT_FINISHED,

		TEST_CASE_RESULT_FINISHED,

		TEST_MODULE_RESULT_FINISHED,

		TEST_TASK_RESULT_FINISHED,
	}
	private ResultListenerState currentState = ResultListenerState.READY;
	private AbstractTestTaskProgress taskProgress;

	void setTaskProgress(final AbstractTestTaskProgress taskProgress) {
		this.taskProgress = taskProgress;
	}

	@Override final public String start(final String testModelItemId, final long startTimestamp) throws IllegalArgumentException, IllegalStateException {
		try {
			switch (currentState) {
			case READY:
				this.currentState = ResultListenerState.WRITING_TEST_TASK_RESULT;
				return startTestTaskResult(testModelItemId, startTimestamp);
			case WRITING_TEST_TASK_RESULT:
				this.currentState = ResultListenerState.WRITING_TEST_MODULE_RESULT;
				return startTestModuleResult(testModelItemId, startTimestamp);
			case WRITING_TEST_MODULE_RESULT:
				this.currentState = ResultListenerState.WRITING_TEST_CASE_RESULT;
				return startTestCaseResult(testModelItemId, startTimestamp);
			case WRITING_TEST_CASE_RESULT:
				this.currentState = ResultListenerState.WRITING_TEST_STEP_RESULT;
				return startTestStepResult(testModelItemId, startTimestamp);
			case WRITING_TEST_STEP_RESULT:
				this.currentState = ResultListenerState.WRITING_TEST_ASSERTION_RESULT;
				return startTestAssertionResult(testModelItemId, startTimestamp);
			}
			throw new IllegalArgumentException("Illegal state: "+currentState);
		}catch (final Exception e) {
			errorLogger.error("An internal error occurred starting result {} ",testModelItemId, e);
			throw new IllegalStateException(e);
		}
	}

	@Override final public String start(final String testModelItemId) throws IllegalArgumentException, IllegalStateException {
		return start(testModelItemId, System.currentTimeMillis());
	}

	abstract protected String startTestTaskResult(final String resultedFrom, final long startTimestamp) throws Exception;

	abstract protected String startTestModuleResult(final String resultedFrom, final long startTimestamp) throws Exception;

	abstract protected  String startTestCaseResult(final String resultedFrom, final long startTimestamp) throws Exception;

	abstract protected  String startTestStepResult(final String resultedFrom, final long startTimestamp) throws Exception;

	abstract protected  String startTestAssertionResult(final String resultedFrom, final long startTimestamp) throws Exception;

	@Override final public String end(final String testModelItemId, final int status, final long stopTimestamp) throws IllegalArgumentException, IllegalStateException {
		try {
			switch (currentState) {
			case TEST_MODULE_RESULT_FINISHED:
				this.currentState=ResultListenerState.TEST_TASK_RESULT_FINISHED;
				return finishTestTaskResult(testModelItemId, status, stopTimestamp);
			case TEST_CASE_RESULT_FINISHED:
				this.currentState=ResultListenerState.TEST_MODULE_RESULT_FINISHED;
				return finishTestModuleResult(testModelItemId, status, stopTimestamp);
			case TEST_STEP_RESULT_FINISHED:
				this.currentState=ResultListenerState.TEST_CASE_RESULT_FINISHED;
				return finishTestCaseResult(testModelItemId, status, stopTimestamp);
			case TEST_ASSERTION_RESULT_FINISHED:
				this.currentState=ResultListenerState.TEST_STEP_RESULT_FINISHED;
				return finishTestStepResult(testModelItemId, status, stopTimestamp);
			case WRITING_TEST_ASSERTION_RESULT:
				this.currentState=ResultListenerState.TEST_ASSERTION_RESULT_FINISHED;
				if (taskProgress!=null) {
					taskProgress.advance();
				}
				return finishTestAssertionResult(testModelItemId, status, stopTimestamp);
			}
			throw new IllegalArgumentException("Illegal state: "+currentState);
		}catch (final Exception e) {
			errorLogger.error("An internal error occurred finishing result {} ",testModelItemId, e);
			throw new IllegalStateException(e);
		}
	}

	@Override final public String end(final String testModelItemId, final int status) throws IllegalArgumentException, IllegalStateException {
		return end(testModelItemId, status, System.currentTimeMillis());
	}

	abstract protected String finishTestTaskResult(final String testModelItemId, final int status, final long stopTimestamp) throws Exception;

	abstract protected String finishTestModuleResult(final String testModelItemId, final int status, final long stopTimestamp) throws Exception;

	abstract protected String finishTestCaseResult(final String testModelItemId, final int status, final long stopTimestamp) throws Exception;

	abstract protected String finishTestStepResult(final String testModelItemId, final int status, final long stopTimestamp) throws Exception;

	abstract protected String finishTestAssertionResult(final String testModelItemId, final int status, final long stopTimestamp) throws Exception;

}
