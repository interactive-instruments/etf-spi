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

import static de.interactive_instruments.etf.test.TestDtos.TR_DTO_1;
import static de.interactive_instruments.etf.test.TestDtos.TTR_DTO_1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import de.interactive_instruments.IFile;
import de.interactive_instruments.ReflectionUtils;
import de.interactive_instruments.etf.dal.dao.DataStorageRegistry;
import de.interactive_instruments.etf.dal.dao.StreamWriteDao;
import de.interactive_instruments.etf.dal.dao.WriteDao;
import de.interactive_instruments.etf.dal.dto.result.TestTaskResultDto;
import de.interactive_instruments.etf.dal.dto.run.TestRunDto;
import de.interactive_instruments.etf.dal.dto.run.TestTaskDto;
import de.interactive_instruments.etf.test.DataStorageTestUtils;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.config.ConfigurationException;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestRunTaskTest {

	private static DefaultTestRun testRun;

	@BeforeClass
	public static void setUp()
			throws InvalidStateTransitionException, InitializationException, ConfigurationException, IOException {
		DataStorageTestUtils.ensureInitialization();
	}

	@Test
	public void test1_createTestTask() throws IOException {
		final IFile testRunDir = IFile.createTempDir("etf-unittest");
		final IFile testRunLogDir = testRunDir.expandPath("log");
		testRunLogDir.mkdirs();
		final TestRunLogger runLogger = new DefaultTestRunLogger(testRunLogDir, "default");
		testRun = new DefaultTestRun(TR_DTO_1, runLogger, testRunDir);
		final TestTaskDto testTaskDto = TR_DTO_1.getTestTasks().get(0);
		final UnitTestTestTask testTask = new UnitTestTestTask(0, testTaskDto);
		testRun.setTestTasks(Collections.singletonList(testTask));
		testTask.setResulPersistor(
				new DefaultTestTaskPersistor(testTaskDto,
						TestResultCollectorFactory.getDefault().createTestResultCollector(runLogger, testTaskDto),
						((StreamWriteDao<TestTaskResultDto>) (DataStorageRegistry.instance().get("default")
								.getDao(TestTaskResultDto.class)))));
	}

	@Test
	public void test2_checkInitNotCalled() {
		// submit task
		final TaskPoolRegistry registry = new TaskPoolRegistry(1, 1);
		registry.submitTask(testRun);

		TestRunDto result = null;
		boolean exceptionThrown = false;
		try {
			result = testRun.waitForResult();
		} catch (ExecutionException | InterruptedException e) {
			if (e.getCause() instanceof InvalidStateTransitionException) {
				exceptionThrown = true;
			}
		}
		assertTrue(exceptionThrown);
	}

	@Test(expected = IllegalStateException.class)
	public void test3_checkStart() throws ConfigurationException, InvalidStateTransitionException, InitializationException {
		// submit task
		final TaskPoolRegistry registry = new TaskPoolRegistry(1, 1);
		// Exception call back object already set
		registry.submitTask(testRun);
	}

	@Test
	public void test4_checkStart()
			throws ConfigurationException, InvalidStateTransitionException, InitializationException, IOException {
		test1_createTestTask();

		// submit task
		final TaskPoolRegistry registry = new TaskPoolRegistry(1, 1);
		registry.submitTask(testRun);
		testRun.init();

		TestRunDto result = null;
		try {
			result = testRun.waitForResult();
		} catch (ExecutionException | InterruptedException e) {
			e.printStackTrace();
		}
		assertNotNull(result);
		assertEquals(TTR_DTO_1.getId(), result.getTestTaskResults().get(0).getId());
	}
}
