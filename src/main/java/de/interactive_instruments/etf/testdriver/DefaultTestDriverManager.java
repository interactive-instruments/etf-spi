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
import de.interactive_instruments.etf.component.ComponentInfo;
import de.interactive_instruments.etf.component.ComponentNotLoadedException;
import de.interactive_instruments.etf.dal.dao.WriteDao;
import de.interactive_instruments.etf.dal.dto.IncompleteDtoException;
import de.interactive_instruments.etf.dal.dto.capabilities.TestObjectTypeDto;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static de.interactive_instruments.etf.EtfConstants.ETF_ATTACHMENT_DIR;
import static de.interactive_instruments.etf.EtfConstants.ETF_DATA_STORAGE_NAME;
import static de.interactive_instruments.etf.EtfConstants.ETF_TESTDRIVERS_DIR;

/**
 *
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public class DefaultTestDriverManager implements TestDriverManager {

	final private ConfigProperties configProperties = new ConfigProperties(ETF_DATA_STORAGE_NAME, ETF_TESTDRIVERS_DIR, ETF_ATTACHMENT_DIR);
	protected TestDriverLoader loader;
	private boolean initialized = false;
	private WriteDao<ExecutableTestSuiteDto> etsWriteDao;
	private WriteDao<TestObjectTypeDto> testObjectTypeDtoWriteDao;
	private final Logger logger = LoggerFactory.getLogger(DefaultTestDriverManager.class);
	// private DataStorage dataStorageCallback;

	@Override public List<ComponentInfo> getTestDriverInfo() {
		return loader.getTestDrivers().stream().map(TestDriver::getInfo).collect(Collectors.toList());
	}

	@Override public ConfigPropertyHolder getConfigurationProperties() {
		return configProperties;
	}

	@Override public void init() throws ConfigurationException, InitializationException {
		configProperties.expectAllRequiredPropertiesSet();
		try {
			loader = new TestDriverLoader(configProperties.getPropertyAsFile(ETF_TESTDRIVERS_DIR));
			loader.setConfig(configProperties);
			loader.load();
			initialized = true;
		}catch (final ComponentLoadingException e) {
			throw new InitializationException(e);
		}
	}

	@Override public boolean isInitialized() {
		return initialized;
	}

	@Override public void unload(final EID testDriverId) {
		loader.release(testDriverId.getId());
	}

	@Override public void reload(final EID testDriverId) throws ComponentLoadingException, ConfigurationException {
		loader.reload(testDriverId.getId());
	}


	@Override public void release() {
		loader.release();
		initialized = false;
	}

	@Override public TestRun createTestRun(final TestRunDto testRunDto) throws TestRunInitializationException {
		try {
			testRunDto.ensureBasicValidity();

			final IFile testRunAttachmentDir = configProperties.getPropertyAsFile(ETF_ATTACHMENT_DIR).
					secureExpandPathDown(testRunDto.getId().toString());
			if (testRunAttachmentDir.exists()) {
				throw new IllegalStateException("The attachment directory already exists");
			}
			if (!testRunAttachmentDir.mkdir()) {
				throw new IllegalStateException("Could not create attachment directory");
			}
			final IFile tmpDir = testRunAttachmentDir.
					secureExpandPathDown("tmp");
			tmpDir.mkdir();

			final TestRun testRun = new DefaultTestRun(testRunDto, testRunAttachmentDir);
			final Logger trLogger = testRun.getTaskProgress().getLogger();
			trLogger.info("Preparing Test Run {} (initiated {})", testRun.getLabel(), testRunDto.getStartTimestamp());
			trLogger.info("Resolving Executable Test Suite dependencies");

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
				final DependencyGraph<ExecutableTestSuiteDto> dependencyGraph =
						new DependencyGraph<>(etsLookupSet.getResolved());
				// does not include the base ETS
				final List<ExecutableTestSuiteDto> sortedEts = dependencyGraph.sort();

				// Add new test tasks
				for (int i = sortedEts.size() - 1; i >= 0; i--) {
					final TestTaskDto testTaskCopy = new TestTaskDto(testTaskDto);
					testTaskCopy.setId(EidFactory.getDefault().createRandomId());
					testTaskCopy.setExecutableTestSuite(sortedEts.get(i));
					testTaskCopy.setTestTaskResult(null);
					testTaskCopy.setParent(testRunDto);
					reorganizedTestTasks.add(testTaskCopy);
				}
			}
			testRunDto.setTestTasks(Collections.unmodifiableList(reorganizedTestTasks));

			// TODO optimize out tasks with equal TO, ETS and parameters

			if(testRunDto.getTestTasks().size()==1) {
				trLogger.info("Preparing 1 Test Task:");
			}else{
				trLogger.info("Preparing {} Test Task:", testRunDto.getTestTasks().size());
			}
			final List<TestTask> testTasks = new ArrayList<>();
			int counter = 0;
			for (final TestTaskDto testTaskDto : testRunDto.getTestTasks()) {
				trLogger.info(" TestTask {} ({})", ++counter, testTaskDto.getId());
				trLogger.info(" will perform tests on Test Object '{}' by using Executable Test Suite {}", testTaskDto.getTestObject().getLabel(), testTaskDto.getExecutableTestSuite().getDescriptiveLabel());
				if (testTaskDto.getArguments() != null) {
					trLogger.info(" with parameters: ");
					testTaskDto.getArguments().values().entrySet().forEach(p -> trLogger.info("{} = {}", p.getKey(), p.getValue()));
				}
				final TestDriver tD = loader.getTestDriverById(testTaskDto.getExecutableTestSuite().getTestDriver().getId().toString());
				final TestTask testTask = tD.createTestTask(testTaskDto);

				testTask.setResultListener(new DefaultResultListener(testRun.getTaskProgress().getLogger(), tmpDir, testRunAttachmentDir));
				testTasks.add(testTask);
			}
			((DefaultTestRun) testRun).setTestTasks(testTasks);
			trLogger.info("Test Tasks prepared and ready to be executed. Waiting for the scheduler to start.");
			return testRun;
		}catch (TestTaskInitializationException | IncompleteDtoException | ComponentNotLoadedException | ConfigurationException | ObjectWithIdNotFoundException e) {
			throw new TestRunInitializationException(e);
		}
	}


}
