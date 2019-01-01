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

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
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
			} catch (IOException e) {
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
