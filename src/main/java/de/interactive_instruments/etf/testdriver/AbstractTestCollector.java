/**
 * Copyright 2010-2017 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
public abstract class AbstractTestCollector implements BasicTestResultCollector {

	protected AbstractTestCollector subCollector = null;
	protected final static Logger logger = LoggerFactory.getLogger(TestResultCollector.class);

	@Override
	public String startTestTask(final String resultedFrom, final long startTimestamp)
			throws IllegalArgumentException, IllegalStateException {
		throw new UnsupportedOperationException(
				"Operation not supported by collector, illegal delegation from parent collector");
	}

	@Override
	public String startTestModule(final String resultedFrom, final long startTimestamp)
			throws IllegalArgumentException, IllegalStateException {
		throw new UnsupportedOperationException(
				"Operation not supported by collector, illegal delegation from parent collector");
	}

	protected String startTestCaseResult(final String resultedFrom, final long startTimestamp) throws Exception {
		throw new UnsupportedOperationException(
				"Operation not supported by collector, illegal delegation from parent collector");
	}

	protected String endTestCaseResult(final String testModelItemId, final int status, final long stopTimestamp)
			throws Exception {
		throw new UnsupportedOperationException(
				"Operation not supported by collector, illegal delegation from parent collector");
	}

	abstract protected String startTestStepResult(final String resultedFrom, final long startTimestamp) throws Exception;

	abstract protected String endTestStepResult(final String testModelItemId, final int status, final long stopTimestamp)
			throws Exception;

	abstract protected String startTestAssertionResult(final String resultedFrom, final long startTimestamp) throws Exception;

	abstract protected String endTestAssertionResult(final String testModelItemId, final int status, final long stopTimestamp)
			throws Exception;

	abstract protected void startInvokedTests();

	abstract protected void endInvokedTests();

	abstract protected void startTestAssertionResults();

	abstract protected void endTestAssertionResults();

	protected void notifyError() {
		logger.error("Releasing collector due to an error");
		release();
	}

	abstract void prepareSubCollectorRelease();

	/**
	 * Called by a SubCollector
	 */
	final void releaseSubCollector() {
		prepareSubCollectorRelease();
		mergeResultFromCollector(subCollector);
		subCollector = null;
	}

	abstract protected AbstractTestCollector createCalledTestCaseResultCollector(final AbstractTestCollector parentCollector,
			final String testModelItemId, final long startTimestamp);

	abstract protected AbstractTestCollector createCalledTestStepResultCollector(final AbstractTestCollector parentCollector,
			final String testModelItemId, final long startTimestamp);

	abstract protected void mergeResultFromCollector(final AbstractTestCollector collector);

	abstract protected String currentResultItemId();
}
