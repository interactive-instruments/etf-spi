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
package de.interactive_instruments.etf.testdriver;

import java.util.Collection;
import java.util.Set;

import de.interactive_instruments.etf.dal.dto.test.ExecutableTestSuiteDto;
import de.interactive_instruments.etf.model.DefaultEidSet;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.etf.model.EidSet;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.config.ConfigurationException;
import de.interactive_instruments.properties.ConfigProperties;
import de.interactive_instruments.properties.ConfigPropertyHolder;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public abstract class AbstractTestDriver implements TestDriver {

	protected EtsTypeLoader typeLoader;
	protected ExecutableTestSuiteLifeCycleListenerMediator mediator;
	final protected ConfigProperties configProperties;
	private boolean initialized = false;

	protected AbstractTestDriver(final ConfigProperties configProperties) {
		this.configProperties = configProperties;
	}

	@Override
	public void lookupExecutableTestSuites(final EtsLookupRequest etsLookupRequest) {
		final Set<EID> etsIds = etsLookupRequest.getUnknownEtsIds();
		if (etsIds != null && !etsIds.isEmpty()) {
			final EidSet<ExecutableTestSuiteDto> knownEts = new DefaultEidSet<>();
			for (final EID etsId : etsIds) {
				final ExecutableTestSuiteDto ets = typeLoader.getExecutableTestSuiteById(etsId);
				if (ets != null) {
					knownEts.add(ets);
				}
			}
			etsLookupRequest.addKnownEts(knownEts);
		}
	}

	@Override
	public ConfigPropertyHolder getConfigurationProperties() {
		return configProperties;
	}

	@Override
	public Collection<ExecutableTestSuiteDto> getExecutableTestSuites() {
		return (Collection<ExecutableTestSuiteDto>) typeLoader.getTypes();
	}

	@Override
	public final void init() throws ConfigurationException, InitializationException, InvalidStateTransitionException {
		if (this.initialized) {
			throw new IllegalStateException("Test Driver already initialized");
		}
		this.configProperties.expectAllRequiredPropertiesSet();
		doInit();
		if (this.typeLoader != null) {
			if (this.mediator != null) {
				this.typeLoader.setLifeCycleListener(this.mediator);
				if (this.typeLoader instanceof ExecutableTestSuiteLifeCycleListener) {
					this.mediator.registerListener((ExecutableTestSuiteLifeCycleListener) this.typeLoader);
				}
			}
			// Initialization is done here to properly set the Life Cycle mediator first
			this.typeLoader.init();
		}
		this.initialized = true;
	}

	protected abstract void doInit() throws ConfigurationException, InitializationException, InvalidStateTransitionException;

	@Override
	public final boolean isInitialized() {
		return initialized;
	}

	@Override
	public final void release() {
		doRelease();
		if (this.typeLoader != null) {
			this.typeLoader.release();
			if (this.mediator != null && this.typeLoader instanceof ExecutableTestSuiteLifeCycleListener) {
				this.mediator.deregisterListener((ExecutableTestSuiteLifeCycleListener) this.typeLoader);
			}
		}
		this.initialized = false;
	}

	protected abstract void doRelease();

	@Override
	public void setLifeCycleMediator(final ExecutableTestSuiteLifeCycleListenerMediator mediator) {
		this.mediator = mediator;
		if (this.typeLoader != null) {
			this.typeLoader.setLifeCycleListener(this.mediator);
		}
	}
}
