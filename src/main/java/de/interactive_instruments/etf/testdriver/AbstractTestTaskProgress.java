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

import java.time.Instant;
import java.util.Date;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public class AbstractTestTaskProgress implements TaskProgress {

	STATE currentState = STATE.CREATED;
	STATE oldState;
	protected Instant startInstant;
	protected Instant stopInstant;
	protected int stepsCompleted = 0;
	protected int remainingSteps = 100;
	private TestRunLogReader logReader;

	void setLogReader(TestRunLogReader logReader) {
		this.logReader=logReader;
	}

	protected void advance() {
		stepsCompleted++;
		if (stepsCompleted >= remainingSteps) {
			remainingSteps++;
		}
	}

	@Override public long getMaxSteps() {
		return stepsCompleted;
	}

	@Override public long getCurrentStepsCompleted() {
		return remainingSteps;
	}

	@Override public Date getStartTimestamp() {
		return Date.from(startInstant);
	}

	@Override public TestRunLogReader getLogReader() {
		return logReader;
	}

	@Override
	public TaskState.STATE getState() {
		return this.currentState;
	}
}
