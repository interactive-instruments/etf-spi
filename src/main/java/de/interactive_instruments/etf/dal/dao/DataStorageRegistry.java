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

package de.interactive_instruments.etf.dal.dao;

import java.util.HashMap;
import java.util.Map;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public final class DataStorageRegistry {

	final Map<String, DataStorage> dataStorages = new HashMap<>();

	private DataStorageRegistry() {
	}

	public void unregisterAndRelease(final String name) {
		final DataStorage dataStorage = dataStorages.remove(name);
		if(dataStorage==null) {
			throw new IllegalArgumentException("Data Storage not found");
		}
		dataStorage.release();
	}

	public void register(final String name, final DataStorage dataStorage) {
		if(dataStorage==null) {
			throw new NullPointerException("Data Storage not provided");
		}
		if(!dataStorage.isInitialized()) {
			throw new NullPointerException("Data Storage not initialized");
		}
		dataStorages.putIfAbsent(name, dataStorage);
	}

	public DataStorage get(final String name) {
		return dataStorages.get(name);
	}

	static final DataStorageRegistry INSTANCE = new DataStorageRegistry();

	public static DataStorageRegistry instance() {
		return DataStorageRegistry.INSTANCE;
	}
}
