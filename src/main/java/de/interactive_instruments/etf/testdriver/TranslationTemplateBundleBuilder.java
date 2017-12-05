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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

import javax.xml.xpath.XPathExpressionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.interactive_instruments.SUtils;
import de.interactive_instruments.etf.XmlUtils;
import de.interactive_instruments.etf.dal.dao.Dao;
import de.interactive_instruments.etf.dal.dao.StreamWriteDao;
import de.interactive_instruments.etf.dal.dto.translation.TranslationTemplateBundleDto;
import de.interactive_instruments.exceptions.ExcUtils;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public final class TranslationTemplateBundleBuilder
		implements TypeBuildingFileVisitor.TypeBuilder<TranslationTemplateBundleDto> {

	private final StreamWriteDao<TranslationTemplateBundleDto> writeDao;
	private final static Logger logger = LoggerFactory.getLogger(TranslationTemplateBundleBuilder.class);
	private static final String TRANSLATION_TEMPLATE_BUNDLE_PREFIX = "TranslationTemplateBundle-";
	private static final String TRANSLATION_TEMPLATE_BUNDLE_SUFFIX = ".xml";

	public TranslationTemplateBundleBuilder(final Dao<TranslationTemplateBundleDto> writeDao) {
		this.writeDao = (StreamWriteDao<TranslationTemplateBundleDto>) writeDao;
	}

	private static class TranslationTemplateBuilderCmd
			extends TypeBuildingFileVisitor.TypeBuilderCmd<TranslationTemplateBundleDto> {

		private final StreamWriteDao<TranslationTemplateBundleDto> writeDao;
		private final static Logger logger = LoggerFactory.getLogger(TranslationTemplateBuilderCmd.class);

		TranslationTemplateBuilderCmd(final Path path, final StreamWriteDao<TranslationTemplateBundleDto> writeDao)
				throws IOException, XPathExpressionException {
			super(path);
			this.writeDao = writeDao;
			this.id = XmlUtils.eval("/etf:TranslationTemplateBundle[1]/@id", path.toFile());
			try {
				dependsOn(XmlUtils.eval(
						"/etf:TranslationTemplateBundle[1]/etf:parent/@ref", path.toFile()));
			} catch (final IOException ignoreNotFound) {
				ExcUtils.suppress(ignoreNotFound);
			}
		}

		@Override
		protected TranslationTemplateBundleDto build() {
			try {
				final File file = path.toFile();
				final FileInputStream fileInputStream = new FileInputStream(file);
				return writeDao.add(fileInputStream, dto -> {
					dto.setSource(file.toURI());
					return dto;
				});
			} catch (IOException e) {
				logger.error("Error creating Translation Template Bundle from file {}", path, e);
			}
			return null;
		}
	}

	@Override
	public TypeBuildingFileVisitor.TypeBuilderCmd<TranslationTemplateBundleDto> prepare(final Path path) {
		final String fName = path.getFileName().toString();
		if (fName.startsWith(TRANSLATION_TEMPLATE_BUNDLE_PREFIX) &&
				fName.endsWith(TRANSLATION_TEMPLATE_BUNDLE_SUFFIX)) {
			try {
				return new TranslationTemplateBuilderCmd(path, writeDao);
			} catch (IOException | XPathExpressionException e) {
				logger.error("Could not prepare Translation Template Bundle {} ", path, e);
			}
		}
		return null;
	}
}
