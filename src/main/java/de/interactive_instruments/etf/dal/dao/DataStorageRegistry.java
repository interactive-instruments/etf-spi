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
package de.interactive_instruments.etf.dal.dao;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public final class DataStorageRegistry {

	final Map<String, DataStorage> dataStorages = new LinkedHashMap<>();

	private DataStorageRegistry() {}

	public void unregisterAndRelease(final String name) {
		final DataStorage dataStorage = dataStorages.remove(name);
		if (dataStorage == null) {
			throw new IllegalArgumentException("Data Storage not found");
		}
		dataStorage.release();
	}

	public void register(final DataStorage dataStorage) {
		if (dataStorage == null) {
			throw new NullPointerException("Data Storage not provided");
		}
		if (!dataStorage.isInitialized()) {
			throw new NullPointerException("Data Storage not initialized");
		}
		dataStorages.putIfAbsent(dataStorage.getClass().getName(), dataStorage);
	}

	public DataStorage get(final String name) {
		if(dataStorages.isEmpty()) {
			return null;
		}
		if ("default".equals(name)) {
			return dataStorages.values().iterator().next();
		}
		return dataStorages.get(name);
	}

	static final DataStorageRegistry INSTANCE = new DataStorageRegistry();

	public static DataStorageRegistry instance() {
		return DataStorageRegistry.INSTANCE;
	}
}
