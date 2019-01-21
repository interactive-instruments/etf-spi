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

import java.io.InputStream;
import java.util.Objects;

import de.interactive_instruments.etf.dal.dao.StreamWriteDao;
import de.interactive_instruments.etf.dal.dto.result.TestTaskResultDto;
import de.interactive_instruments.etf.dal.dto.run.TestTaskDto;
import de.interactive_instruments.exceptions.StorageException;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class DefaultTestTaskPersistor implements TestTaskResultPersistor, TestTaskEndListener {

    private final TestTaskDto testTaskDto;
    private final TestResultCollector collector;
    private final StreamWriteDao<TestTaskResultDto> writeDao;
    private boolean persisted = false;

    public DefaultTestTaskPersistor(
            final TestTaskDto testTaskDto,
            final TestResultCollector collector,
            final StreamWriteDao<TestTaskResultDto> writeDao) {
        this.testTaskDto = Objects.requireNonNull(testTaskDto, "Test Task is null");
        this.writeDao = Objects.requireNonNull(writeDao, "DAO is null");
        this.collector = Objects.requireNonNull(collector, "Collector is null");
        this.collector.registerTestTaskEndListener(this);
    }

    private void checkState() {
        if (resultPersisted()) {
            throw new IllegalStateException("Result already persisted");
        }
    }

    private void setResultInTask(final TestTaskResultDto testTaskResultDto) {
        testTaskDto.setTestTaskResult(testTaskResultDto);
        persisted = true;
    }

    @Override
    public TestResultCollector getResultCollector() {
        return collector;
    }

    @Override
    public void streamResult(final InputStream resultStream) throws StorageException {
        checkState();
        setResultInTask(writeDao.add(resultStream));
    }

    @Override
    public void setResult(final TestTaskResultDto testTaskResultDto) throws StorageException {
        checkState();
        writeDao.add(testTaskResultDto);
        setResultInTask(testTaskResultDto);
    }

    @Override
    public boolean resultPersisted() {
        return persisted;
    }

    @Override
    public void testTaskFinished(final TestTaskResultDto testTaskResultDto) {
        setResultInTask(testTaskResultDto);
    }
}
