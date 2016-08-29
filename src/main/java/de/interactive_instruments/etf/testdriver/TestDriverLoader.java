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

package de.interactive_instruments.etf.testdriver;

import de.interactive_instruments.IFile;
import de.interactive_instruments.Releasable;
import de.interactive_instruments.etf.component.ComponentInfo;
import de.interactive_instruments.etf.component.ComponentNotLoadedException;
import de.interactive_instruments.etf.dal.dao.DataStorage;
import de.interactive_instruments.exceptions.config.ConfigurationException;
import de.interactive_instruments.properties.Properties;
import de.interactive_instruments.properties.PropertyHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Test Driver Loader which manages and loads the Test Drivers from Jar files
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
class TestDriverLoader implements Releasable {


    private final ConcurrentMap<String, ComponentContainer> driverContainer = new ConcurrentHashMap<>(8);
    private final ConcurrentMap<String, TestDriver> testDrivers = new ConcurrentHashMap<>(8);
    private final ConcurrentMap<String, PropertyHolder> specificConfigurations = new ConcurrentHashMap<>(8);
    private PropertyHolder configuration = new Properties();
    private final IFile testDriverDir;
    private Logger logger = LoggerFactory.getLogger(TestDriverLoader.class);
    private long testDriverLasModified=0;

    private static class TestDriverJarFileFilter implements FilenameFilter {

        @Override
        public boolean accept(final File dir, String name) {
            return name.endsWith("jar");
        }
    }

    public TestDriverLoader(final IFile testDriverDir) throws ComponentLoadingException {
        this.testDriverDir=testDriverDir;
        recreateTestComponents();
    }

    private void recreateTestComponents() throws ComponentLoadingException {
        for (IFile driverDir : testDriverDir.listDirs()) {
            final IFile driverBinDir = driverDir.expandPath("bin");
            if(driverBinDir.exists() && driverBinDir.listFiles().length>0) {
                final File jarFile = driverBinDir.listFiles(new TestDriverJarFileFilter())[0];
                if(jarFile==null) {
                    throw new ComponentLoadingException("No test driver found in "+driverBinDir.getAbsolutePath());
                }
                if(isTestComponentPrepared(jarFile)) {
                    continue;
                }
                final ComponentContainer testDriverContainer = new ComponentContainer(
                        jarFile,
                        driverDir.expandPath("lib").listFiles());
                this.driverContainer.put(testDriverContainer.getId(), testDriverContainer);
            }
        }
        testDriverLasModified=testDriverDir.lastModified();
    }

    private boolean isTestComponentPrepared(final File jarFile) {
        for (ComponentContainer testDriverContainer : driverContainer.values()) {
            if(testDriverContainer.getJar().equals(jarFile)) {
                // driver already loaded
                return true;
            }
        }
        return false;
    }

    public void setSpecificConfig(String id, PropertyHolder config) {
        this.specificConfigurations.put(id, config);
    }

    public void setConfig(PropertyHolder config) {
        this.configuration=config;
    }

    /**
     * Load component
     * @param id
     */
    public synchronized void load(String id) throws ComponentLoadingException, ConfigurationException {
        if(this.testDrivers.containsKey(id)) {
            throw new ComponentLoadingException("TestDriver "+id+" already loaded");
        }
        final PropertyHolder config;
        if(specificConfigurations.containsKey(id)) {
            config=specificConfigurations.get(id);
        }else{
            config=this.configuration;
        }
        logger.info("Loading test driver \"{}\"",id);
        final ComponentContainer driverContainer = this.driverContainer.get(id);
        if(!driverContainer.getJar().exists()) {
            logger.info("Jar {} for test driver {} not found, parsing test drivers directory again",
                    id, driverContainer.getJar());
            driverContainer.release();
            recreateTestComponents();
        }
        this.testDrivers.put(id, this.driverContainer.get(id).loadAndInit(config));
    }

    /**
     * Load all components
     */
    public synchronized void load() throws ConfigurationException, ComponentLoadingException {
        if(testDriverDir.lastModified()!=testDriverLasModified) {
            recreateTestComponents();
        }
        for (final ComponentContainer testDriverContainer : this.driverContainer.values()) {
            final String id = testDriverContainer.getId();
            load(id);
        }
    }

    /**
     * Unload component
     * @param id
     */
    public synchronized void release(final String id) {
        logger.info("Releasing test driver \"{}\"",id);
        this.driverContainer.get(id).release();
        this.testDrivers.remove(id);
    }

    /**
     * Release all components
     */
    @Override
    public synchronized void release() {
        this.driverContainer.keySet().forEach(id -> release(id));
    }

    /**
     * Reload component
     * @param id
     */
    public synchronized void reload(String id) throws ComponentLoadingException, ConfigurationException {
        release(id);
        load(id);
    }

    public ComponentInfo getInfo(final String id) {
        return this.driverContainer.get(id).getInfo();
    }

    public Collection<ComponentInfo> getInfo() {
        final ArrayList<ComponentInfo> i = new ArrayList();
        this.driverContainer.values().forEach(d ->
        {
            if (d.getInfo() != null) {
                i.add(d.getInfo());
            }
        });
        if(i.isEmpty()) {
            return null;
        }
        return i;
    }

    public Collection<TestDriver> getTestDrivers() {
        return Collections.unmodifiableCollection(testDrivers.values());
    }

    public TestDriver getTestDriverById(final String id) throws ConfigurationException, ComponentNotLoadedException {
        final TestDriver factory = testDrivers.get(id);
        if(factory==null) {
            throw new ComponentNotLoadedException("Unknown TestRunTaskFactory "+id);
        }
        return factory;
    }

}
