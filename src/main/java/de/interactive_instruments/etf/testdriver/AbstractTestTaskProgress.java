/**
 * Copyright 2017-2019 European Union, interactive instruments GmbH
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * This work was supported by the EU Interoperability Solutions for
 * European Public Administrations Programme (http://ec.europa.eu/isa)
 * through Action 1.17: A Reusable INSPIRE Reference Platform (ARE3NA).
 */
package de.interactive_instruments.etf.testdriver;

import java.time.Instant;
import java.util.Date;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class AbstractTestTaskProgress implements TaskProgress {

	private STATE currentState = STATE.CREATED;
	private STATE oldState = null;
	protected Instant startInstant;
	protected Instant stopInstant;
	private long stepsCompleted = 0;
	private long maxSteps = -1;
	private TestRunLogReader logReader;

	void setLogReader(TestRunLogReader logReader) {
		this.logReader = logReader;
	}

	protected void initMaxSteps(final long maxSteps) {
		if (this.maxSteps != -1) {
			throw new IllegalArgumentException("Max steps already set");
		}
		if (maxSteps <= 0) {
			throw new IllegalArgumentException("Invalid max value");
		}
		this.maxSteps = maxSteps;
	}

	protected void advance() {
		stepsCompleted++;
		if (stepsCompleted >= maxSteps) {
			maxSteps = stepsCompleted + 1;
		}
	}

	void setState(final STATE currentState) {
		this.oldState = this.currentState;
		this.currentState = currentState;
	}

	public STATE getCurrentState() {
		return currentState;
	}

	public STATE getOldState() {
		return oldState;
	}

	@Override
	public long getMaxSteps() {
		return maxSteps;
	}

	@Override
	public long getCurrentStepsCompleted() {
		return stepsCompleted;
	}

	@Override
	public Date getStartTimestamp() {
		return Date.from(startInstant);
	}

	@Override
	public TestRunLogReader getLogReader() {
		return logReader;
	}

	@Override
	public TaskState.STATE getState() {
		return this.currentState;
	}
}
