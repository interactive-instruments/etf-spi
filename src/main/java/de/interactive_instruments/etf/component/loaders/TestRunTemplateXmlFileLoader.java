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
package de.interactive_instruments.etf.component.loaders;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

import javax.xml.xpath.XPathExpressionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.interactive_instruments.etf.EtfXpathEvaluator;
import de.interactive_instruments.etf.dal.dao.Dao;
import de.interactive_instruments.etf.dal.dao.StreamWriteDao;
import de.interactive_instruments.etf.dal.dto.capabilities.TestRunTemplateDto;
import de.interactive_instruments.exceptions.ExcUtils;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StorageException;

/**
 * A loader for Test Run Templates stored in XML files
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
final class TestRunTemplateXmlFileLoader extends AbstractItemFileLoaderFactory<TestRunTemplateDto> {

    private final StreamWriteDao<TestRunTemplateDto> writeDao;
    private final static Logger logger = LoggerFactory.getLogger(TestRunTemplateXmlFileLoader.class);
    private static final String TEST_RUN_TEMPLATE_PREFIX = "TestRunTemplate-";
    private static final String TEST_RUN_TEMPLATE_SUFFIX = ".xml";
    private static final int priority = 500;

    TestRunTemplateXmlFileLoader(final Dao<TestRunTemplateDto> writeDao) {
        this.writeDao = (StreamWriteDao<TestRunTemplateDto>) writeDao;
    }

    private static class TestRunTemplateLoadCmd extends AbstractItemFileLoader<TestRunTemplateDto> {
        private final StreamWriteDao<TestRunTemplateDto> writeDao;

        TestRunTemplateLoadCmd(final ItemFileLoaderResultListener<TestRunTemplateDto> itemListener,
                final Path path, final StreamWriteDao<TestRunTemplateDto> writeDao) throws XPathExpressionException {
            super(itemListener, priority, path.toFile());
            this.writeDao = writeDao;
        }

        @Override
        protected boolean doPrepare() {
            try {
                dependsOn(EtfXpathEvaluator.evalEids(
                        "/etf:TestRunTemplate[1]/etf:executableTestSuites[1]/etf:executableTestSuite/@ref", file));

                dependsOn(EtfXpathEvaluator.evalEids(
                        "/etf:TestRunTemplate[1]/etf:testObjects[1]/etf:testObject/@ref", file));

            } catch (final IOException | XPathExpressionException e) {
                logger.error("Error preparing Test Run Template from file {}", file, e);
                return false;
            }
            return true;
        }

        @Override
        protected TestRunTemplateDto doBuild() {
            try {
                return writeDao.add(new FileInputStream(file));
            } catch (IOException e) {
                logger.error("Error creating Test Run Template from file {}", file, e);
            }
            return null;
        }

        @Override
        protected void doRelease() {
            if (getResult() != null) {
                try {
                    writeDao.delete(getResult().getId());
                } catch (StorageException | ObjectWithIdNotFoundException e) {
                    ExcUtils.suppress(e);
                }
            }
        }
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public boolean couldHandle(final Path path) {
        final String fName = path.getFileName().toString();
        return fName.startsWith(TEST_RUN_TEMPLATE_PREFIX) && fName.endsWith(TEST_RUN_TEMPLATE_SUFFIX);
    }

    @Override
    public FileChangeListener load(final Path path) {
        if (couldHandle(path)) {
            try {
                return new TestRunTemplateXmlFileLoader.TestRunTemplateLoadCmd(
                        this, path, writeDao).setItemRegistry(getItemRegistry());
            } catch (XPathExpressionException e) {
                logger.error("Could not prepare Tag {} ", path, e);
            }
        }
        return null;
    }
}
