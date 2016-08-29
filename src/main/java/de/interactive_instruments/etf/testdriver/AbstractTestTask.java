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

import de.interactive_instruments.IFile;
import de.interactive_instruments.etf.dal.dto.result.TestTaskResultDto;
import de.interactive_instruments.etf.dal.dto.run.TestTaskDto;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.exceptions.ExcUtils;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.config.ConfigurationException;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public abstract class AbstractTestTask implements TestTask {

	final protected TestTaskDto testTaskDto;
	private STATE currentState = STATE.CREATED;
	private STATE oldState;
	private ArrayList<TaskStateEventListener> eventListeners;
	protected Instant startInstant;
	protected Instant stopInstant;
	private boolean initialized;
	protected int stepsCompleted = 0;
	protected int remainingSteps = 100;

	protected TestResultListener resultListener;

	protected AbstractTestTask(final TestTaskDto testTaskDto) {
		this.testTaskDto = testTaskDto;
	}

	@Override public EID getId() {
		return testTaskDto.getId();
	}



	@Override public final void run() throws Exception {
		fireInitializing();
		doRun();
		fireCompleted();
	}

	protected abstract void doRun() throws Exception;

	@Override public TestTaskResultDto getResult() {
		return testTaskDto.getTestTaskResult();
	}

	protected abstract void doInit() throws ConfigurationException, InitializationException, InvalidStateTransitionException;

	@Override
	public final void init() throws ConfigurationException, InvalidStateTransitionException, InitializationException {
		doInit();
		getLogger().info("TestRunTask initialized");
	}

	@Override
	public boolean isInitialized() {
		return this.initialized;
	}

	protected abstract void doRelease();

	@Override
	public final void release() {
		try {
			fireFinalizing();
			getLogger().info("Releasing resources");
			doRelease();
		} catch (InvalidStateTransitionException e) {
			ExcUtils.suppress(e);
		} catch (Exception e) {
			getLogger().warn("Releasing of resource failed: " + e.getMessage());
		}
	}

	protected abstract void doCancel() throws InvalidStateTransitionException;

	protected void checkCancelStatus() throws InterruptedException {
		if (this.currentState == TaskState.STATE.CANCELING ||
				Thread.currentThread().isInterrupted()) {
			try {
				cancel();
			} catch (InvalidStateTransitionException e) {
				getLogger().error("Unable to cancel task: " + e.getMessage());
			}
			throw new InterruptedException();
		}
	}

	@Override
	public final void cancel() throws InvalidStateTransitionException {
		getLogger().info("Canceling TestRunTask." + getId());
		fireCanceling();
		doCancel();
		fireCanceled();
		release();
		getLogger().info("TestRunTask." + getId() + " canceled");
	}

	@Override
	public long getMaxSteps() {
		return remainingSteps;
	}

	@Override
	public long getCurrentStepsCompleted() {
		return stepsCompleted;
	}

	public double getPercentStepsCompleted() {
		return  getCurrentStepsCompleted()/getMaxSteps()*100;
	}

	@Override public TestRunLogger getLogger() {
		return resultListener.getLogger();
	}

	// STATE implementations
	///////////////////////////

	@Override
	public STATE getState() {
		return this.currentState;
	}

	/*
	@Override
	public synchronized void addStateEventListener(TaskStateEventListener listener) {
		if(this.eventListeners==null) {
			this.eventListeners=new ArrayList<>();
		}
		this.eventListeners.add(listener);
	}
	*/

	final private void changeState(STATE state, boolean reqCondition) throws InvalidStateTransitionException {
		if(!reqCondition || this.currentState==state) {
			final String errorMsg = "Illegal state transition in task "+this.testTaskDto.getId()+
					" from "+this.currentState+" to "+state;
			getLogger().error(errorMsg);
			throw new InvalidStateTransitionException(errorMsg);
		}
		if(this.oldState!=null) {
			// getLogger().info("Changed state from {} to {}", this.oldState, this.currentState);
		}else{
			// getLogger().info("Setting state to {}", this.currentState);
		}
		synchronized (this) {
			this.oldState = this.currentState;
			this.currentState = state;
			if (this.eventListeners != null) {
				this.eventListeners.forEach(l ->
						l.taskStateChangedEvent(this, this.currentState, this.oldState));
			}
		}
	}

	@Override public void setResultListener(final TestResultListener listener) {
		this.resultListener = listener;
	}

	/**
	 * Sets the start timestamp and the state to INITIALIZING
	 * @throws InvalidStateTransitionException
	 */
	private void fireInitializing() throws InvalidStateTransitionException {
		changeState(STATE.INITIALIZING,
				(currentState==STATE.CREATED));
		this.startInstant= Instant.now();
	}

	protected final void fireInitialized() throws InvalidStateTransitionException {
		changeState(STATE.INITIALIZED,
				(currentState==STATE.INITIALIZING));
	}

	protected final void fireRunning() throws InvalidStateTransitionException {
		changeState(STATE.RUNNING,
				(currentState==STATE.INITIALIZED));
	}

	private void fireCompleted() throws InvalidStateTransitionException {
		changeState(STATE.COMPLETED,
				(currentState==STATE.RUNNING));
		this.stopInstant=Instant.now();
	}


	final void fireFinalizing() throws InvalidStateTransitionException {
		changeState(STATE.FINALIZING,
				(currentState==STATE.COMPLETED ||
						currentState==STATE.CANCELED ||
						currentState==STATE.FAILED));
	}

	/**
	 * Puts the task into a final state.
	 *
	 * Does not throw an exception!
	 */
	final void fireFailed()  {
		try {
			changeState(STATE.FAILED,
					(currentState==STATE.CREATED) ||
							(currentState==STATE.INITIALIZING) ||
							(currentState==STATE.INITIALIZED)  ||
							(currentState==STATE.RUNNING));
		} catch (InvalidStateTransitionException e) {
			ExcUtils.suppress(e);
		}
		this.stopInstant=Instant.now();
	}

	final protected void fireCanceling() throws InvalidStateTransitionException {
		changeState(STATE.CANCELING,
				(currentState==STATE.CREATED ||
						currentState==STATE.INITIALIZING ||
						currentState==STATE.INITIALIZED ||
						currentState==STATE.RUNNING) );
	}

	/**
	 * Puts the task into a final state.
	 *
	 * @throws InvalidStateTransitionException
	 */
	final void fireCanceled() throws InvalidStateTransitionException {
		changeState(STATE.CANCELED,
				(currentState==STATE.CANCELING));
		this.stopInstant=Instant.now();
	}

	@Override public Date getStartTimestamp() {
		return Date.from(startInstant);
	}
}
