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
import de.interactive_instruments.etf.dal.dto.translation.TranslationTemplateBundleDto;
import de.interactive_instruments.exceptions.ExcUtils;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StorageException;

/**
 * A loader for Translation Template Bundles stored in XML files
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
final class TranslationTemplateBundleXmlFileLoader
        extends AbstractItemFileLoaderFactory<TranslationTemplateBundleDto> {

    private final StreamWriteDao<TranslationTemplateBundleDto> writeDao;
    private final static Logger logger = LoggerFactory.getLogger(TranslationTemplateBundleXmlFileLoader.class);
    private static final String TRANSLATION_TEMPLATE_BUNDLE_PREFIX = "TranslationTemplateBundle-";
    private static final String TRANSLATION_TEMPLATE_BUNDLE_SUFFIX = ".xml";
    private static final int priority = 200;

    TranslationTemplateBundleXmlFileLoader(final Dao<TranslationTemplateBundleDto> writeDao) {
        this.writeDao = (StreamWriteDao<TranslationTemplateBundleDto>) writeDao;
    }

    private static class TranslationTemplateBundleLoadCmd extends AbstractItemFileLoader<TranslationTemplateBundleDto> {

        private final StreamWriteDao<TranslationTemplateBundleDto> writeDao;

        TranslationTemplateBundleLoadCmd(final ItemFileLoaderResultListener<TranslationTemplateBundleDto> itemListener,
                final Path path, final StreamWriteDao<TranslationTemplateBundleDto> writeDao) {
            super(itemListener, priority, path.toFile());
            this.writeDao = writeDao;
        }

        @Override
        protected boolean doPrepare() {
            try {
                dependsOn(EtfXpathEvaluator.evalEidOrNull(
                        "/etf:TranslationTemplateBundle[1]/etf:parent/@ref", file));
            } catch (final IOException | XPathExpressionException e) {
                logger.error("Error preparing Translation Template Bundle from file {}", file, e);
                return false;
            }
            return true;
        }

        @Override
        protected TranslationTemplateBundleDto doBuild() {
            try {
                return writeDao.add(new FileInputStream(file), dto -> {
                    dto.setSource(file.toURI());
                    return dto;
                });
            } catch (IOException e) {
                logger.error("Error creating Translation Template Bundle from file {}", file, e);
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
        return fName.startsWith(TRANSLATION_TEMPLATE_BUNDLE_PREFIX) &&
                fName.endsWith(TRANSLATION_TEMPLATE_BUNDLE_SUFFIX);
    }

    @Override
    public FileChangeListener load(final Path path) {
        if (couldHandle(path)) {
            return new TranslationTemplateBundleLoadCmd(
                    this, path, writeDao).setItemRegistry(getItemRegistry());
        }
        return null;
    }
}
