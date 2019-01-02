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

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
/*
public class AbstractTypeLoader implements TypeLoader {

	protected final DataStorage dataStorageCallback;
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private final ClassLoader contextClassloader;
	private boolean initialized = false;
	private final EidHolderMap cachedTypes = new DefaultEidHolderMap();

	protected AbstractTypeLoader(final DataStorage dataStorageCallback) {
		this.dataStorageCallback = dataStorageCallback;
		this.contextClassloader = Thread.currentThread().getContextClassLoader();
	}

	protected void ensureContextClassLoader() {
		Thread.currentThread().setContextClassLoader(contextClassloader);
	}

	protected abstract void doBeforeDeregister(final EidSet<? extends Dto> dtos);

	protected final void deregisterTypes(final EidSet<Dto> values) {
		cachedTypes.removeAll(values);
		doBeforeDeregister(values);
			try {
				((WriteDao) dataStorageCallback.getDao(dto.getClass())).delete(dto.getId());
			} catch (ObjectWithIdNotFoundException | StorageException e) {
				logger.error("Could not deregister {} : ", dto.getDescriptiveLabel(), e);
			}
		});
	}

	protected abstract void doBeforeRegister(final EidSet<? extends Dto> dtos);

	protected abstract void doAfterRegister(final EidSet<? extends Dto> dtos);

	protected void registerTypes(final EidSet<Dto> dtos) {
		// Register IDs
		doBeforeRegister(dtos);
		cachedTypes.addAll(dtos);
		doAfterRegister(dtos);
	}

	@Override
	public final EidSet<? extends Dto> getTypes() {
		return cachedTypes.toSet();
	}

	protected abstract void doInit() throws ConfigurationException, InitializationException, InvalidStateTransitionException;

	@Override
	public final void init() throws InitializationException, InvalidStateTransitionException, ConfigurationException {
		if (initialized == true) {
			throw new InvalidStateTransitionException("Already initialized");
		}

		doInit();

		this.initialized = true;
	}
}
*/
