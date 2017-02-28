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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.interactive_instruments.Configurable;
import de.interactive_instruments.IFile;
import de.interactive_instruments.Releasable;
import de.interactive_instruments.etf.dal.dao.DataStorage;
import de.interactive_instruments.etf.dal.dao.WriteDao;
import de.interactive_instruments.etf.dal.dto.Dto;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StorageException;
import de.interactive_instruments.exceptions.config.ConfigurationException;
import de.interactive_instruments.io.DirWatcher;
import de.interactive_instruments.io.FileChangeListener;
import de.interactive_instruments.io.MultiFileFilter;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public abstract class AbstractTypeLoader implements Configurable, Releasable, FileChangeListener {

	protected final DataStorage dataStorageCallback;

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	private boolean initialized = false;
	protected IFile watchDir;
	private final List<TypeBuildingFileVisitor.TypeBuilder<? extends Dto>> builders;

	// Path -> Dto
	protected final Map<Path, Dto> propagatedDtos = new LinkedHashMap<>();

	// Synced between all AbstractTypeLoaders
	private final static Set<String> globalRegisteredTypeIds = new ConcurrentSkipListSet<>();

	protected AbstractTypeLoader(final DataStorage dataStorageCallback,
			final List<TypeBuildingFileVisitor.TypeBuilder<? extends Dto>> builders) {
		this.dataStorageCallback = dataStorageCallback;
		this.builders = builders;
	}

	protected abstract void doBeforeDeregister(final Dto dto);

	private void deregisterTypes(final Collection<Dto> values) {
		values.stream().forEach(dto -> {
			doBeforeDeregister(dto);

			globalRegisteredTypeIds.remove(dto.getId().getId());

			try {
				((WriteDao) dataStorageCallback.getDao(dto.getClass())).delete(dto.getId());
			} catch (ObjectWithIdNotFoundException | StorageException e) {
				logger.error("Could not deregister {} : ", dto.getDescriptiveLabel(), e);
			}
		});
	}

	protected abstract void doAfterRegister(final Collection<Dto> dtos);

	private void registerTypes(final Collection<Dto> values) {
		// Register ID
		values.stream().forEach(dto -> globalRegisteredTypeIds.add(dto.getId().getId()));
		doAfterRegister(values);
	}

	@Override
	public final synchronized void filesChanged(final Map<Path, WatchEvent.Kind> eventMap, final Set<Path> dirs) {
		final Set<Path> parentLessDirs = dirs.stream().filter(dir -> !dirs.contains(dir.getParent()))
				.collect(Collectors.toSet());

		// Check which files were removed
		final List<Dto> dtosToRemove = propagatedDtos.entrySet().stream().filter(entry -> !Files.exists(entry.getKey()))
				.map(Map.Entry::getValue).collect(Collectors.toList());
		if (!dtosToRemove.isEmpty()) {
			deregisterTypes(dtosToRemove);
		}

		doBeforeVisit(dirs);

		// Create Types
		final TypeBuildingFileVisitor visitor = new TypeBuildingFileVisitor(builders, globalRegisteredTypeIds);
		parentLessDirs.forEach(d -> {
			logger.trace("Watch service reports changes in directory: " + d.toString());
			try {
				Files.walkFileTree(d, visitor);
			} catch (IOException e) {
				logger.error("Failed to walk path tree: " + e.getMessage());
			}
		});
		final Map<Path, Dto> newPropagatedDtos = visitor.buildAll();
		if (newPropagatedDtos != null) {
			registerTypes(newPropagatedDtos.values());
			propagatedDtos.putAll(newPropagatedDtos);
		}
	}

	protected abstract void doBeforeVisit(final Set<Path> dirs);

	protected abstract void doInit() throws ConfigurationException, InitializationException, InvalidStateTransitionException;

	@Override
	public final void init() throws InitializationException, InvalidStateTransitionException, ConfigurationException {
		if (initialized == true) {
			throw new InvalidStateTransitionException("Already initialized");
		}

		doInit();

		// Initial parse dir
		filesChanged(null, Collections.singleton(watchDir.toPath()));

		// Start watching the directory
		DirWatcher.register(this.watchDir.toPath(), this);

		this.initialized = true;
	}

	@Override
	public MultiFileFilter fileChangePreFilter() {
		return (File pathname) -> !pathname.getName().startsWith(".");
	}

	@Override
	public final boolean isInitialized() {
		return this.initialized;
	}

	protected abstract void doRelease();

	@Override
	public void release() {
		doRelease();
		DirWatcher.unregister(this);
		this.propagatedDtos.clear();
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer(getClass().getSimpleName());
		sb.append(" (");
		sb.append(this.propagatedDtos.size());
		sb.append(')');
		return sb.toString();
	}
}
