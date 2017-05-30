/**
 * Copyright 2010-2017 interactive instruments GmbH
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
import de.interactive_instruments.etf.dal.dto.capabilities.TestObjectTypeDto;
import de.interactive_instruments.exceptions.StorageException;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public final class TestObjectTypeBuilder implements TypeBuildingFileVisitor.TypeBuilder<TestObjectTypeDto> {

	private final StreamWriteDao<TestObjectTypeDto> writeDao;
	private final static Logger logger = LoggerFactory.getLogger(TestObjectTypeBuilder.class);
	private static final String TEST_OBJECT_TYPE_PREFIX = "TestObjectType-";
	private static final String TEST_OBJECT_TYPE_SUFFIX = ".xml";

	public TestObjectTypeBuilder(final Dao<TestObjectTypeDto> writeDao) {
		this.writeDao = (StreamWriteDao<TestObjectTypeDto>) writeDao;
	}

	private static class TestObjectTypeBuilderCmd extends TypeBuildingFileVisitor.TypeBuilderCmd<TestObjectTypeDto> {

		private final StreamWriteDao<TestObjectTypeDto> writeDao;
		private final static Logger logger = LoggerFactory.getLogger(TestObjectTypeBuilderCmd.class);

		TestObjectTypeBuilderCmd(final Path path, final StreamWriteDao<TestObjectTypeDto> writeDao)
				throws IOException, XPathExpressionException {
			super(path);
			this.writeDao = writeDao;
			this.id = XmlUtils.eval("/etf:TestObjectType[1]/@id", path.toFile());
		}

		@Override
		protected TestObjectTypeDto build() {
			try {
				final File file = path.toFile();
				final FileInputStream fileInputStream = new FileInputStream(file);
				return writeDao.add(fileInputStream);
			} catch (IOException e) {
				logger.error("Error creating Test Object Type from file {}", path, e);
			}
			return null;
		}
	}

	@Override
	public TypeBuildingFileVisitor.TypeBuilderCmd<TestObjectTypeDto> prepare(final Path path) {
		final String fName = path.getFileName().toString();
		if (fName.startsWith(TEST_OBJECT_TYPE_PREFIX) &&
				fName.endsWith(TEST_OBJECT_TYPE_SUFFIX)) {
			try {
				return new TestObjectTypeBuilderCmd(path, writeDao);
			} catch (IOException | XPathExpressionException e) {
				logger.error("Could not prepare Test Object Type {} ", path, e);
			}

		}
		return null;
	}
}
