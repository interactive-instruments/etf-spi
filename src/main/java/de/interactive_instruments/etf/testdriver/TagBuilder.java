/**
 * Copyright 2010-2016 interactive instruments GmbH
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
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

import javax.xml.xpath.XPathExpressionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.interactive_instruments.etf.XmlUtils;
import de.interactive_instruments.etf.dal.dao.Dao;
import de.interactive_instruments.etf.dal.dao.StreamWriteDao;
import de.interactive_instruments.etf.dal.dto.capabilities.TagDto;
import de.interactive_instruments.exceptions.StorageException;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public final class TagBuilder implements TypeBuildingFileVisitor.TypeBuilder<TagDto> {

	private final StreamWriteDao<TagDto> writeDao;
	private final static Logger logger = LoggerFactory.getLogger(TagBuilder.class);
	private static final String TAG_PREFIX = "Tag-";
	private static final String TAG_SUFFIX = ".xml";

	public TagBuilder(final Dao<TagDto> writeDao) {
		this.writeDao = (StreamWriteDao<TagDto>) writeDao;
	}

	private static class TagBuilderCmd extends TypeBuildingFileVisitor.TypeBuilderCmd<TagDto> {

		private final StreamWriteDao<TagDto> writeDao;
		private final static Logger logger = LoggerFactory.getLogger(TagDto.class);

		TagBuilderCmd(final Path path, final StreamWriteDao<TagDto> writeDao) throws IOException, XPathExpressionException {
			super(path);
			this.writeDao = writeDao;
			this.id = XmlUtils.eval("/etf:Tag[1]/@id", path.toFile());
		}

		@Override
		protected TagDto build() {
			try {
				final File file = path.toFile();
				final FileInputStream fileInputStream = new FileInputStream(file);
				return writeDao.add(fileInputStream);
			} catch (IOException | StorageException e) {
				logger.error("Error creating Tag from file {}", path, e);
			}
			return null;
		}
	}

	@Override
	public TypeBuildingFileVisitor.TypeBuilderCmd<TagDto> prepare(final Path path) {
		final String fName = path.getFileName().toString();
		if (fName.startsWith(TAG_PREFIX) && fName.endsWith(TAG_SUFFIX)) {
			try {
				return new TagBuilderCmd(path, writeDao);
			} catch (IOException | XPathExpressionException e) {
				logger.error("Could not prepare Tag {} ", path, e);
			}
		}
		return null;
	}
}
