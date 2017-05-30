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

import static de.interactive_instruments.etf.EtfConstants.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import de.interactive_instruments.SUtils;
import de.interactive_instruments.etf.model.EidHolderMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.interactive_instruments.IFile;
import de.interactive_instruments.etf.component.ComponentInfo;
import de.interactive_instruments.etf.component.ComponentLoadingException;
import de.interactive_instruments.etf.component.ComponentNotLoadedException;
import de.interactive_instruments.etf.dal.dto.IncompleteDtoException;
import de.interactive_instruments.etf.dal.dto.run.TestRunDto;
import de.interactive_instruments.etf.dal.dto.run.TestTaskDto;
import de.interactive_instruments.etf.dal.dto.test.ExecutableTestSuiteDto;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.etf.model.EidFactory;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.config.ConfigurationException;
import de.interactive_instruments.properties.ConfigProperties;
import de.interactive_instruments.properties.ConfigPropertyHolder;

/**
 *
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class DefaultTestDriverManager implements TestDriverManager {

	final private ConfigProperties configProperties = new ConfigProperties(ETF_DATA_STORAGE_NAME, ETF_TESTDRIVERS_DIR,
			ETF_ATTACHMENT_DIR);
	protected TestDriverLoader loader;
	private boolean initialized = false;
	private final Logger logger = LoggerFactory.getLogger(DefaultTestDriverManager.class);

	@Override
	public List<ComponentInfo> getTestDriverInfo() {
		return loader.getTestDrivers().stream().map(TestDriver::getInfo).collect(Collectors.toList());
	}

	@Override
	public void loadAll() throws ComponentLoadingException, ConfigurationException {
		loader.load();
	}

	@Override
	public void load(final EID testDriverId)
			throws ObjectWithIdNotFoundException, ComponentLoadingException, ConfigurationException {
		loader.load(testDriverId.getId());
	}

	@Override
	public ConfigPropertyHolder getConfigurationProperties() {
		return configProperties;
	}

	@Override
	public void init() throws ConfigurationException, InitializationException {
		configProperties.expectAllRequiredPropertiesSet();
		try {
			configProperties.getPropertyAsFile(ETF_ATTACHMENT_DIR).expectDirIsWritable();
			final IFile testDriversDir = configProperties.getPropertyAsFile(ETF_TESTDRIVERS_DIR);
			testDriversDir.expectDirIsReadable();
			loader = new TestDriverLoader(testDriversDir, new LifeCycleListenerMediator());
		} catch (IOException e) {
			throw new InitializationException(e);
		}
		loader.setConfig(configProperties);

		initialized = true;
	}

	@Override
	public boolean isInitialized() {
		return initialized;
	}

	@Override
	public void unload(final EID testDriverId) {
		loader.release(testDriverId.getId());
	}

	@Override
	public void reload(final EID testDriverId) throws ComponentLoadingException, ConfigurationException {
		loader.reload(testDriverId.getId());
	}

	@Override
	public void release() {
		if (loader != null) {
			loader.release();
		}
		initialized = false;
	}

	private static void addUnique(final TestTaskDto newTestTask, final List<TestTaskDto> reorganizedTestTasks) {
		for (final TestTaskDto testTask : reorganizedTestTasks) {
			if (testTask.getTestObject().getId().equals(newTestTask.getTestObject().getId()) &&
					testTask.getExecutableTestSuite().getId().equals(newTestTask.getExecutableTestSuite().getId())) {
				// todo compare parameters?
				return;
			}
		}
		reorganizedTestTasks.add(newTestTask);
	}

	@Override
	public TestRun createTestRun(final TestRunDto testRunDto, final TestResultCollectorFactory collectorFactory)
			throws TestRunInitializationException {
		try {
			testRunDto.ensureBasicValidity();

			final IFile testRunAttachmentDir = configProperties.getPropertyAsFile(ETF_ATTACHMENT_DIR)
					.secureExpandPathDown(testRunDto.getId().toString());
			if (testRunAttachmentDir.exists()) {
				logger.error("The attachment directory already exists: " + testRunAttachmentDir.getAbsolutePath());
				throw new IllegalStateException("The attachment directory already exists");
			}
			if (!testRunAttachmentDir.mkdir()) {
				logger.error("Could not create attachment directory: " + testRunAttachmentDir.getAbsolutePath());
				throw new IllegalStateException("Could not create attachment directory");
			}
			final IFile tmpDir = testRunAttachmentDir.secureExpandPathDown("tmp");
			tmpDir.mkdir();

			final TestRunLogger testRunLogger = new DefaultTestRunLogger(
					testRunAttachmentDir, "tr-" + testRunDto.getId().getId());
			final TestRun testRun = new DefaultTestRun(testRunDto, testRunLogger, testRunAttachmentDir);
			testRunLogger.info("Preparing Test Run {} (initiated {})", testRun.getLabel(), testRunDto.getStartTimestamp());
			testRunLogger.info("Resolving Executable Test Suite dependencies");

			final List<TestTaskDto> reorganizedTestTasks = new ArrayList<>();

			// create test tasks for dependencies
			for (final TestTaskDto testTaskDto : testRunDto.getTestTasks()) {
				final EtsLookupSet etsLookupSet = new EtsLookupSet(testTaskDto);
				// Get for each ETS the responsible test drivers
				// max 8 tries
				int tries = 0;
				while (etsLookupSet.hasUnknownEts() && tries < 8) {
					final EID driverId = etsLookupSet.getNextTestDriverId();
					final TestDriver tD = loader.getTestDriverById(driverId.getId());
					tD.lookupExecutableTestSuites(etsLookupSet.etsResolver(driverId));
					++tries;
				}

				// Create dependency graph
				final DependencyGraph<ExecutableTestSuiteDto> dependencyGraph = new DependencyGraph<>(
						etsLookupSet.getResolved());
				// does not include the base ETS
				final List<ExecutableTestSuiteDto> sortedEts = dependencyGraph.sortIgnoreCylce();

				// Add new test tasks
				for (int i = sortedEts.size() - 1; i >= 0; i--) {
					final TestTaskDto testTaskCopy = testTaskDto.createCopy();
					testTaskCopy.setId(EidFactory.getDefault().createRandomId());
					testTaskCopy.setExecutableTestSuite(sortedEts.get(i));
					testTaskCopy.setTestTaskResult(null);
					testTaskCopy.setParent(testRunDto);
					addUnique(testTaskCopy, reorganizedTestTasks);
				}
			}
			testRunDto.setTestTasks(Collections.unmodifiableList(reorganizedTestTasks));

			if (testRunDto.getTestTasks().size() == 1) {
				testRunLogger.info("Preparing 1 Test Task:");
			} else {
				testRunLogger.info("Preparing {} Test Task:", testRunDto.getTestTasks().size());
			}
			final List<TestTask> testTasks = new ArrayList<>();
			int counter = 0;
			for (final TestTaskDto testTaskDto : testRunDto.getTestTasks()) {
				testRunLogger.info(" TestTask {} ({})", ++counter, testTaskDto.getId());
				testRunLogger.info(" will perform tests on Test Object '{}' by using Executable Test Suite {}",
						testTaskDto.getTestObject().getLabel(), testTaskDto.getExecutableTestSuite().getDescriptiveLabel());
				if (testTaskDto.getArguments() != null && !testTaskDto.getArguments().values().isEmpty() ) {
					testRunLogger.info(" with parameters: ");
					testTaskDto.getArguments().values().entrySet()
							.forEach(p -> testRunLogger.info("{} = {}", p.getKey(), p.getValue()));
				}
				final TestDriver tD = loader
						.getTestDriverById(testTaskDto.getExecutableTestSuite().getTestDriver().getId().toString());
				final TestTask testTask = tD.createTestTask(testTaskDto);

				testTask.setResultListener(collectorFactory.createTestResultCollector(testRunLogger, testTaskDto));
				testTasks.add(testTask);
			}
			((DefaultTestRun) testRun).setTestTasks(testTasks);
			testRunLogger.info("Test Tasks prepared and ready to be executed. Waiting for the scheduler to start.");
			return testRun;
		} catch (TestTaskInitializationException | IncompleteDtoException | ComponentNotLoadedException | ConfigurationException
				| ObjectWithIdNotFoundException e) {
			throw new TestRunInitializationException(e);
		}
	}

	private class LifeCycleListenerMediator implements ExecutableTestSuiteLifeCycleListenerMediator {

		final Set<ExecutableTestSuiteLifeCycleListener> listeners = new HashSet<>();

		@Override
		public void registerListener(final ExecutableTestSuiteLifeCycleListener executableTestSuiteLifeCycleListener) {
			listeners.add(executableTestSuiteLifeCycleListener);
		}

		@Override
		public void deregisterListener(final ExecutableTestSuiteLifeCycleListener executableTestSuiteLifeCycleListener) {
			listeners.remove(executableTestSuiteLifeCycleListener);
		}

		@Override
		public void lifeCycleChange(final Object caller, final EventType eventType, final EidHolderMap<ExecutableTestSuiteDto> eidHolderMap) {
			for (final ExecutableTestSuiteLifeCycleListener listener : listeners) {
				if(!listener.equals(caller)) {
					listener.lifeCycleChange(caller, eventType, eidHolderMap);
				}
			}
		}
	}
}
