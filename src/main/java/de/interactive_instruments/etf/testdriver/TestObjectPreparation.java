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

import de.interactive_instruments.etf.dal.dto.capabilities.TestObjectDto;
import de.interactive_instruments.exceptions.InitializationException;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public interface TestObjectPreparation {

	/**
	 * Init Test Object
	 *
	 * @param testObjectDto
	 */
	void init(final TestObjectDto testObjectDto) throws InitializationException;

	/**
	 * Background task after initial creation
	 *
	 * @param testObject
	 * @return
	 */
	Task<TestObjectDto> initialPrepareTask(final TestObjectDto testObject);

	/**
	 * Executed before a Test Run
	 *
	 * @param testObjectDto
	 */
	void prepareBeforeRun(final TestObjectDto testObjectDto) throws InitializationException;
}
