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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.interactive_instruments.etf.dal.dao.Dao;
import de.interactive_instruments.etf.dal.dao.StreamWriteDao;
import de.interactive_instruments.etf.dal.dto.capabilities.TagDto;
import de.interactive_instruments.exceptions.ExcUtils;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StorageException;

/**
 * A loader for Tags stored in XML files
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
final class TagXmlFileLoader extends AbstractItemFileLoaderFactory<TagDto> {

    private final StreamWriteDao<TagDto> writeDao;
    private final static Logger logger = LoggerFactory.getLogger(TagXmlFileLoader.class);
    private static final String TAG_PREFIX = "Tag-";
    private static final String TAG_SUFFIX = ".xml";
    private static final int priority = 100;

    TagXmlFileLoader(final Dao<TagDto> writeDao) {
        this.writeDao = (StreamWriteDao<TagDto>) writeDao;
    }

    private static class TagLoadCmd extends AbstractItemFileLoader<TagDto> {

        private final StreamWriteDao<TagDto> writeDao;

        TagLoadCmd(final ItemFileLoaderResultListener<TagDto> itemListener,
                final Path path, final StreamWriteDao<TagDto> writeDao) {
            super(itemListener, priority, path.toFile());
            this.writeDao = writeDao;
        }

        @Override
        protected boolean doPrepare() {
            return true;
        }

        @Override
        protected TagDto doBuild() {
            try {
                return writeDao.add(new FileInputStream(file));
            } catch (IOException e) {
                logger.error("Error creating Tag from file {}", file, e);
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
        return fName.startsWith(TAG_PREFIX) && fName.endsWith(TAG_SUFFIX);
    }

    @Override
    public FileChangeListener load(final Path path) {
        if (couldHandle(path)) {
            return new TagLoadCmd(
                    this, path, writeDao).setItemRegistry(getItemRegistry());
        }
        return null;
    }
}
