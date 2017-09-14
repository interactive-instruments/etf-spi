/**
 * Copyright 2017 European Union, interactive instruments GmbH
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
package de.interactive_instruments.etf.testdriver.detection;

import java.util.Collection;

import de.interactive_instruments.MediaType;
import de.interactive_instruments.etf.dal.dto.capabilities.TestObjectDto;
import de.interactive_instruments.etf.dal.dto.capabilities.TestObjectTypeDto;
import de.interactive_instruments.etf.model.EidHolder;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public interface TypeDetector extends EidHolder {

	Collection<MediaType> supportedTypes();

	interface TypeDetectionCmd {
		enum Status {
			TYPE_KNOWN, NEED_MORE_DATA, TYPE_UNKNOWN,
		}

		Status status();

		/**
		 * Set the Test Object type for the Test Object
		 *
		 * @param dto Test Object Dto
		 */
		void setType(final TestObjectDto dto);

		/**
		 * All Types a Detector can detect
		 *
		 * @return list of TestObjectTypeDto
		 */
		Collection<TestObjectTypeDto> getDetectibleTypes();
	}

	boolean supportsDetectionByMimeType();

	boolean supportsDetectionByFileExtension();

	boolean supportsDetectionByContent();
}
