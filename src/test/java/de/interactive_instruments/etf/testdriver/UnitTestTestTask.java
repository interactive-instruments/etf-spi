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

import static de.interactive_instruments.etf.test.TestDtos.TTR_DTO_1;

import de.interactive_instruments.etf.dal.dto.run.TestTaskDto;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.config.ConfigurationException;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class UnitTestTestTask extends AbstractTestTask {

    private final int failInStage;

    protected UnitTestTestTask(
            final int failInStage,
            final TestTaskDto testTaskDto) {
        super(testTaskDto, new UnitTestTestTaskProgress(), UnitTestTestTask.class.getClassLoader());
        this.failInStage = failInStage;
    }

    @Override
    protected void doRun() throws Exception {
        this.progress.advance();
        if (failInStage > 1) {
            throw new IllegalStateException("FAIL");
        }
        this.progress.advance();
    }

    @Override
    protected void doInit() throws ConfigurationException, InitializationException {
        if (failInStage > 2) {
            throw new InitializationException("FAIL");
        }
        testTaskDto.setTestTaskResult(TTR_DTO_1);
    }

    @Override
    protected void doRelease() {
        if (failInStage > 3) {
            throw new IllegalStateException("FAIL");
        }

    }

    @Override
    protected void doCancel() throws InvalidStateTransitionException {
        if (failInStage > 4) {
            throw new InvalidStateTransitionException("FAIL");
        }
    }
}
