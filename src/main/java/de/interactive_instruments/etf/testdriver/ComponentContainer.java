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

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.interactive_instruments.*;
import de.interactive_instruments.etf.component.ComponentInfo;
import de.interactive_instruments.etf.component.ComponentLoadingException;
import de.interactive_instruments.exceptions.ExcUtils;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.config.ConfigurationException;
import de.interactive_instruments.properties.PropertyHolder;

/**
 * A container for the component which holds and manages a loaded jar.
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
final class ComponentContainer implements Releasable {
	private final File componentJar;
	private String id;

	private Class<?> clasz;
	private TestDriver testDriver;
	private Class testDriverInitializerClass;
	private ComponentClassLoader cl;
	private Logger logger = LoggerFactory.getLogger(ComponentContainer.class);
	private static List<String> disallowedJars = Collections.unmodifiableList(
			// List of libraries that are not loaded from the packaged
			// lib folders but rather loaded from the highest layer once
			new ArrayList<String>() {
				{
					// Disallow Xerces
					add("xerces");
					add("xercesImpl");
					add("xml-apis");
					// Disallow logging frameworks
					add("slf4j-api");
					add("slf4j-nop");
					add("slf4j-log4j");
					add("logback-classic");
					add("logback-core");
					// Disallow etf-core and etf-spi
					add("etf-core");
					add("etf-spi");
				}
			});

	ComponentContainer(final File componentJar) throws ComponentLoadingException {
		this.componentJar = componentJar;
		prepareTestDriver();
	}

	private void prepareTestDriver() throws ComponentLoadingException {
		if (testDriverInitializerClass == null) {
			try {
				cl = new ComponentClassLoader(componentJar.toURI().toURL());
				final List<String> classNames = JarUtils.scanForClassNames(componentJar);
				// Find the activator class which is annotated with "ComponentInitializer"
				for (String className : classNames) {
					final Class clasz = cl.loadClass(className);
					final Annotation a = clasz.getAnnotation(ComponentInitializer.class);
					if (a != null) {
						this.id = ((ComponentInitializer) a).id();
						testDriverInitializerClass = clasz;
						break;
					}
				}
				if (testDriverInitializerClass == null) {
					// is the test driver using the same etf-model?
					throw new ComponentLoadingException("Failed to load " + componentJar.toPath() +
							" because no ComponentInitializer annotated class has been found");
				}
			} catch (NullPointerException | NoClassDefFoundError | ClassNotFoundException | IOException e) {
				throw new ComponentLoadingException(componentJar.toPath(), e);
			} finally {
				System.gc();
			}
		}
	}

	TestDriver loadAndInit(
			final PropertyHolder properties,
			final ExecutableTestSuiteLifeCycleListenerMediator mediator)
			throws ComponentLoadingException, ConfigurationException {
		if (this.testDriverInitializerClass == null) {
			final String oldId = this.id;
			prepareTestDriver();
			if (!this.id.equals(oldId)) {
				throw new ComponentLoadingException("Unexpected change of the test driver id");
			}
		}
		if (testDriver != null) {
			throw new ComponentLoadingException("Test driver " + this.id + " already loaded");
		}
		final ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(cl);
			// Construct the new test driver
			testDriver = (TestDriver) testDriverInitializerClass.newInstance();
			try {
				logger.info("Preparing Test Driver {} {} {}", this.testDriver.getInfo().getName(),
						this.testDriver.getInfo().getVersion(),
						logger.isDebugEnabled() ? CLUtils.getManifestAttributeValue(this.testDriver.getClass(), "Build-Date")
								: "");
			} catch (final Exception e) {
				ExcUtils.suppress(e);
			}
			this.testDriver.getConfigurationProperties().setPropertiesFrom(properties, true);
			this.testDriver.setLifeCycleMediator(mediator);
			this.testDriver.init();
		} catch (InvalidStateTransitionException | InitializationException | IllegalAccessException
				| InstantiationException e) {
			throw new ComponentLoadingException(this.componentJar.toPath(), e);
		} finally {
			Thread.currentThread().setContextClassLoader(oldCl);
		}
		return this.testDriver;
	}

	String getId() {
		return id;
	}

	ComponentInfo getInfo() {
		return this.testDriver == null ? null : this.testDriver.getInfo();
	}

	File getJar() {
		return componentJar;
	}

	/**
	 * Releases all resource. prepareTestDriver() method must be called afterwards for reinitialization!
	 */
	@Override
	public void release() {
		if (testDriver != null) {
			testDriver.release();
			testDriver = null;
		}
		clasz = null;
		testDriverInitializerClass = null;
		IoUtils.closeQuietly(cl);
		cl = null;
	}
}
