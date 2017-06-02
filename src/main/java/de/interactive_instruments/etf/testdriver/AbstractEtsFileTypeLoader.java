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

import java.nio.file.Path;
import java.util.*;

import de.interactive_instruments.SUtils;
import de.interactive_instruments.etf.dal.dao.DataStorage;
import de.interactive_instruments.etf.dal.dao.WriteDao;
import de.interactive_instruments.etf.dal.dto.Dto;
import de.interactive_instruments.etf.dal.dto.test.ExecutableTestSuiteDto;
import de.interactive_instruments.etf.model.*;
import de.interactive_instruments.exceptions.ExcUtils;
import de.interactive_instruments.exceptions.StorageException;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public abstract class AbstractEtsFileTypeLoader extends AbstractFileTypeLoader
		implements EtsTypeLoader, ExecutableTestSuiteLifeCycleListener {

	private final EidHolderMap<ExecutableTestSuiteDto> etsCache = new DefaultEidHolderMap<>(new HashMap<>(64));

	// Executable Test Suites with unresolved dependencies
	private final EidHolderMap<ExecutableTestSuiteDto> etsWithUnresolvedDeps = new DefaultEidHolderMap<>();

	private final WriteDao<ExecutableTestSuiteDto> etsDao;
	private ExecutableTestSuiteLifeCycleListener listener;

	/**
	 * File loaders
	 *
	 * @param dataStorageCallback Storage callback
	 * @param builders Type Builders
	 */
	protected AbstractEtsFileTypeLoader(final DataStorage dataStorageCallback,
			final TypeBuildingFileVisitor.TypeBuilder<? extends Dto>... builders) {
		super(dataStorageCallback, Arrays.asList(builders));
		etsDao = (WriteDao<ExecutableTestSuiteDto>) dataStorageCallback.getDao(ExecutableTestSuiteDto.class);
	}

	@Override
	protected void doBeforeDeregister(final Collection<? extends Dto> dtos) {
		if (dtos != null && dtos.iterator().next() instanceof ExecutableTestSuiteDto) {
			etsCache.removeAll(dtos);
			if (this.listener != null) {
				this.listener.lifeCycleChange(
						this,
						ExecutableTestSuiteLifeCycleListener.EventType.REMOVED,
						DefaultEidHolderMap.singleton((ExecutableTestSuiteDto) dtos));
			}
		}
	}

	@Override
	protected void doAfterRegister(final Collection<? extends Dto> dtos) {
		final EidHolderMap<ExecutableTestSuiteDto> etsToCache = new DefaultEidHolderMap<>();
		for (final Dto dto : dtos) {
			if (dto instanceof ExecutableTestSuiteDto) {
				final ExecutableTestSuiteDto ets = (ExecutableTestSuiteDto) dto;
				// Check if the builder already added the ETS and activated it
				if (etsDao.exists(ets.getId()) && !etsDao.isDisabled(ets.getId())) {
					// Just cache the ETS and fire the added event
					etsToCache.add(ets);
				} else {
					// Check for resolved dependencies
					boolean depsResolved = true;
					final Collection<ExecutableTestSuiteDto> deps = ets.getDependencies();
					if (deps != null) {
						for (final ExecutableTestSuiteDto dep : deps) {
							if (SUtils.isNullOrEmpty(dep.getLabel())) {
								depsResolved = false;
								break;
							}
						}
					}
					if (depsResolved) {
						try {
							etsDao.add(ets);
							etsToCache.add(ets);
						} catch (StorageException e) {
							logger.error("Could not add ETS ", e);
						}
					} else {
						markEtsWithUnresolvedDependencies(ets);
					}
				}
			}
		}
		cacheEts(etsToCache);
	}

	private void cacheEts(final EidHolderMap<ExecutableTestSuiteDto> ets) {
		if (!ets.isEmpty()) {
			etsCache.putAll(ets);
			etsWithUnresolvedDeps.removeAll(ets.asCollection());
			if (this.listener != null) {
				this.listener.lifeCycleChange(this,
						ExecutableTestSuiteLifeCycleListener.EventType.CREATED, ets);
			}
		}
	}

	/**
	 * Hold the Ets back until all cross-Test Driver dependencies emerge
	 *
	 * @param ets ETS with unresolved dependencies
	 */
	private void markEtsWithUnresolvedDependencies(final ExecutableTestSuiteDto ets) {
		// Ensure the ETS is not in the cache
		etsCache.remove(ets);
		etsWithUnresolvedDeps.add(ets);
		logger.info("Disabling Test Suite {} until all cross-Test Driver dependencies can be resolved",
				ets.getId());
	}

	@Override
	protected void doRelease() {
		this.etsWithUnresolvedDeps.clear();
		this.etsCache.clear();
	}

	@Override
	public ExecutableTestSuiteDto getExecutableTestSuiteById(final EID id) {
		return etsCache.get(id);
	}

	@Override
	public EidSet<? extends Dto> getTypes() {
		return etsCache.toSet();
	}

	@Override
	protected void doBeforeVisit(final Set<Path> dirs) {
		try {
			// Make sure that other types are created first
			Thread.sleep(3000);
		} catch (InterruptedException ign) {
			ExcUtils.suppress(ign);
		}
	}

	@Override
	public void setLifeCycleListener(final ExecutableTestSuiteLifeCycleListener listener) {
		this.listener = listener;
	}

	@Override
	public synchronized void lifeCycleChange(final Object caller, final EventType eventType, final EidHolderMap changedEts) {

		if (eventType == EventType.CREATED && !this.etsWithUnresolvedDeps.isEmpty()) {
			resolveDep(changedEts);
		} else if (eventType == EventType.REMOVED) {
			// Check if there are ETS in the cache with the dependency
			final List<ExecutableTestSuiteDto> invalidatedEts = new ArrayList<>();
			for (final ExecutableTestSuiteDto etsCacheEntry : etsCache.values()) {
				final EidHolderMap<ExecutableTestSuiteDto> deps = new DefaultEidHolderMap(etsCacheEntry.getDependencies());
				if (deps != null) {
					final EidHolderMap<ExecutableTestSuiteDto> invalidDeps = deps.getAll(changedEts.values());
					invalidDeps.values().forEach(i -> i.setLabel(null));
					markEtsWithUnresolvedDependencies(etsCacheEntry);
				}
			}

		}
	}

	private void resolveDep(final EidMap<ExecutableTestSuiteDto> knownEts) {
		final EidHolderMap<ExecutableTestSuiteDto> etsToAdd = new DefaultEidHolderMap<>();
		for (final ExecutableTestSuiteDto executableTestSuiteDto : etsWithUnresolvedDeps.values()) {
			boolean dependenciesResolved = true;
			final Collection<ExecutableTestSuiteDto> dependencies = executableTestSuiteDto.getDependencies();
			final List<ExecutableTestSuiteDto> resolvedDeps = new ArrayList<>(dependencies.size());
			for (final ExecutableTestSuiteDto dep : dependencies) {
				// dependencies are ExecutableTestSuiteDtos without a label set
				final ExecutableTestSuiteDto resolvedDep = knownEts.get(dep.getId());
				if (resolvedDep == null && SUtils.isNullOrEmpty(dep.getLabel())) {
					dependenciesResolved = false;
					break;
				}
				resolvedDeps.add(resolvedDep);
			}
			if (dependenciesResolved) {
				executableTestSuiteDto.setDependencies(resolvedDeps);
				logger.info("Resolved {} cross-Test Driver dependencies for Executable Test Suite {}",
						resolvedDeps.size(), executableTestSuiteDto.getId());
				etsToAdd.add(executableTestSuiteDto);
			}
		}
		doAfterRegister(etsToAdd.asCollection());
	}
}
