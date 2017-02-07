/**
 * Copyright 2010-2016 interactive instruments GmbH
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

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import de.interactive_instruments.Configurable;
import de.interactive_instruments.Releasable;
import de.interactive_instruments.etf.dal.dao.DataStorage;
import de.interactive_instruments.etf.dal.dto.Dto;
import de.interactive_instruments.etf.dal.dto.test.ExecutableTestSuiteDto;
import de.interactive_instruments.etf.model.DefaultEidMap;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.etf.model.EidMap;
import de.interactive_instruments.exceptions.ExcUtils;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.config.ConfigurationException;
import de.interactive_instruments.io.FileChangeListener;
import de.interactive_instruments.properties.ConfigPropertyHolder;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public abstract class EtsTypeLoader extends AbstractTypeLoader {

	private final EidMap<ExecutableTestSuiteDto> etsCache = new DefaultEidMap<>(new HashMap<>(32));

	protected EtsTypeLoader(final DataStorage dataStorageCallback, final List<TypeBuildingFileVisitor.TypeBuilder<? extends Dto>> builders) {
		super(dataStorageCallback, builders);
	}

	@Override
	protected void doBeforeDeregister(final Dto dto) {
		if (dto instanceof ExecutableTestSuiteDto) {
			etsCache.remove(dto.getId());
		}
	}

	@Override
	protected void doAfterRegister(final Collection<Dto> values) {
		// Add ETS to ETS cache
		values.stream().filter(dto -> dto instanceof ExecutableTestSuiteDto).forEach(
				dto -> etsCache.put(dto.getId(), (ExecutableTestSuiteDto) dto));
	}

	@Override
	protected void doRelease() {
		this.etsCache.clear();
	}

	public Collection<ExecutableTestSuiteDto> getExecutableTestSuites() {
		return etsCache.values();
	}

	public ExecutableTestSuiteDto getExecutableTestSuiteById(final EID id) {
		return etsCache.get(id);
	}

	@Override protected void doBeforeVisit(final Set<Path> dirs) {
		try {
			// Make sure that other types are created first
			Thread.sleep(3000);
		} catch (InterruptedException ign) {
			ExcUtils.suppress(ign);
		}
	}
}
