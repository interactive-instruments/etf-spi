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

import de.interactive_instruments.Configurable;
import de.interactive_instruments.Releasable;
import de.interactive_instruments.etf.dal.dto.Dto;
import de.interactive_instruments.etf.model.EidHolderMap;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public interface TypeLoader extends Configurable, Releasable {

	/**
	 * Return all DTOs the TypeLoader could create after
	 * {@link #init()} has been called.
	 *
	 * @return
	 */
	EidHolderMap<Dto> getTypes();

	/**
	 * Fired after all Test Drivers have been loaded.
	 * Used to reference DTOs across different Test Drivers.
	 * Type loaders exchange their DTOs via {@link #getTypes()}
	 * and complete DTOs within this method. If the TypeLoader has resolved
	 * all dependencies, true must be returned.
	 *
	 * @param types completed types
	 * @return true if all types could be loaded, false if there are still unknown types
	 */
	boolean resolveCrossTestDriverDependencies(final EidHolderMap<Dto> types);
}
