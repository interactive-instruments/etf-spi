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

import de.interactive_instruments.etf.dal.dto.result.TestTaskResultDto;
import de.interactive_instruments.etf.dal.dto.run.TestRunDto;
import de.interactive_instruments.etf.dal.dto.run.TestTaskDto;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.exceptions.ExcUtils;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.config.ConfigurationException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public abstract class AbstractTestTask implements TestTask {

	final protected TestTaskDto testTaskDto;
	private ArrayList<TaskStateEventListener> eventListeners;
	private boolean initialized;
	private Future<TestTaskResultDto> future;
	protected final AbstractTestTaskProgress progress;
	// TODO make private when interface is implemented in BaseX
	protected TestResultCollector resultCollector;

	protected AbstractTestTask(final TestTaskDto testTaskDto, final AbstractTestTaskProgress progress) {
		this.testTaskDto = testTaskDto;
		this.progress = progress;
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
		if(resultCollector ==null) {
			throw new IllegalStateException("Result Listener not set");
		}
		doInit();
		getLogger().info("TestRunTask initialized");
		initialized=true;
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
			initialized=false;
		} catch (InvalidStateTransitionException e) {
			ExcUtils.suppress(e);
		} catch (Exception e) {
			getLogger().warn("Releasing of resource failed: " + e.getMessage());
		}
	}

	protected abstract void doCancel() throws InvalidStateTransitionException;

	protected void checkCancelStatus() throws InterruptedException {
		if (progress.currentState == TaskState.STATE.CANCELING ||
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

	@Override public TestRunLogger getLogger() {
		return resultCollector.getLogger();
	}

	@Override public TaskProgress getProgress() {
		return this.progress;
	}

	@Override public STATE getState() {
		return this.progress.currentState;
	}

	@Override public void setFuture(final Future<TestTaskResultDto> future) throws IllegalStateException {
		if (this.future != null) {
			throw new IllegalStateException(
					"The already set call back object can not be changed!");
		}
		this.future = future;
	}

	@Override public TestTaskResultDto call() throws Exception {
		run();
		return testTaskDto.getTestTaskResult();
	}

	@Override
	public TestTaskResultDto waitForResult() throws InterruptedException, ExecutionException {
		return this.future.get();
	}

	// STATE implementations
	///////////////////////////

	/*
	@Override
	public synchronized void addStateEventListener(TaskStateEventListener listener) {
		if(this.eventListeners==null) {
			this.eventListeners=new ArrayList<>();
		}
		this.eventListeners.add(listener);
	}
	*/

	final private void changeState(TaskState.STATE state, boolean reqCondition) throws InvalidStateTransitionException {
		if(!reqCondition || progress.currentState==state) {
			final String errorMsg = "Illegal state transition in task "+this.testTaskDto.getId()+
					" from "+progress.currentState+" to "+state;
			if(resultCollector !=null) {
				getLogger().error(errorMsg);
			}
			throw new InvalidStateTransitionException(errorMsg);
		}
		if(progress.oldState!=null) {
			// TODO
			// getLogger().info("Changed state from {} to {}", this.oldState, this.currentState);
		}else{
			// TODO
			// getLogger().info("Setting state to {}", this.currentState);
		}
		synchronized (this) {
			progress.oldState = progress.currentState;
			progress.currentState = state;
			if (this.eventListeners != null) {
				this.eventListeners.forEach(l ->
						l.taskStateChangedEvent(this, progress.currentState, progress.oldState));
			}
		}
	}

	@Override public void setResultListener(final TestResultCollector listener) {
		this.resultCollector = listener;
		this.progress.setLogReader(resultCollector.getLogger());
		if (this.resultCollector instanceof AbstractTestResultCollector) {
			((AbstractTestResultCollector) this.resultCollector).setTaskProgress(this.progress);
		}
	}

	/**
	 * Sets the start timestamp and the state to INITIALIZING
	 * @throws InvalidStateTransitionException
	 */
	private void fireInitializing() throws InvalidStateTransitionException {
		changeState(TaskState.STATE.INITIALIZING,
				(progress.currentState== TaskState.STATE.CREATED));
		progress.startInstant=Instant.now();
	}

	protected final void fireInitialized() throws InvalidStateTransitionException {
		changeState(TaskState.STATE.INITIALIZED,
				(progress.currentState== TaskState.STATE.INITIALIZING));
	}

	protected final void fireRunning() throws InvalidStateTransitionException {
		changeState(TaskState.STATE.RUNNING,
				(progress.currentState== TaskState.STATE.INITIALIZED));
	}

	private void fireCompleted() throws InvalidStateTransitionException {
		changeState(TaskState.STATE.COMPLETED,
				(progress.currentState== TaskState.STATE.RUNNING));
		progress.stopInstant=Instant.now();
	}


	final void fireFinalizing() throws InvalidStateTransitionException {
		changeState(TaskState.STATE.FINALIZING,
				(progress.currentState== TaskState.STATE.COMPLETED ||
						progress.currentState== TaskState.STATE.CANCELED ||
						progress.currentState== TaskState.STATE.FAILED));
	}

	/**
	 * Puts the task into a final state.
	 *
	 * Does not throw an exception!
	 */
	final void fireFailed()  {
		try {
			changeState(TaskState.STATE.FAILED,
					(progress.currentState== TaskState.STATE.CREATED) ||
							(progress.currentState== TaskState.STATE.INITIALIZING) ||
							(progress.currentState== TaskState.STATE.INITIALIZED)  ||
							(progress.currentState== TaskState.STATE.RUNNING));
		} catch (InvalidStateTransitionException e) {
			ExcUtils.suppress(e);
		}
		progress.stopInstant=Instant.now();
	}

	final protected void fireCanceling() throws InvalidStateTransitionException {
		changeState(TaskState.STATE.CANCELING,
				(progress.currentState== TaskState.STATE.CREATED ||
						progress.currentState== TaskState.STATE.INITIALIZING ||
						progress.currentState== TaskState.STATE.INITIALIZED ||
						progress.currentState== TaskState.STATE.RUNNING) );
	}

	/**
	 * Puts the task into a final state.
	 *
	 * @throws InvalidStateTransitionException
	 */
	final void fireCanceled() throws InvalidStateTransitionException {
		changeState(TaskState.STATE.CANCELED,
				(progress.currentState== TaskState.STATE.CANCELING));
		progress.stopInstant=Instant.now();
	}
}
