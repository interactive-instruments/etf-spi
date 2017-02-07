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

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import de.interactive_instruments.etf.EtfConstants;
import de.interactive_instruments.etf.dal.dao.DataStorage;
import de.interactive_instruments.etf.dal.dto.Dto;
import de.interactive_instruments.etf.dal.dto.capabilities.TagDto;
import de.interactive_instruments.etf.dal.dto.capabilities.TestObjectTypeDto;
import de.interactive_instruments.etf.dal.dto.test.ExecutableTestSuiteDto;
import de.interactive_instruments.etf.dal.dto.translation.TranslationTemplateBundleDto;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.config.ConfigurationException;
import de.interactive_instruments.properties.ConfigProperties;
import de.interactive_instruments.properties.ConfigPropertyHolder;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public final class MetadataTypeLoader extends AbstractTypeLoader {

	private final ConfigProperties configProperties;

	public MetadataTypeLoader(final DataStorage dataStorageCallback) {
		super(dataStorageCallback, new ArrayList<TypeBuildingFileVisitor.TypeBuilder<? extends Dto>>() {
			{
				add(new TranslationTemplateBundleBuilder(dataStorageCallback.getDao(TranslationTemplateBundleDto.class)));
				add(new TestObjectTypeBuilder(dataStorageCallback.getDao(TestObjectTypeDto.class)));
				add(new TagBuilder(dataStorageCallback.getDao(TagDto.class)));
			}
		});
		this.configProperties = new ConfigProperties(EtfConstants.ETF_PROJECTS_DIR);
	}

	@Override
	protected void doInit() throws ConfigurationException, InitializationException, InvalidStateTransitionException {
		this.configProperties.expectAllRequiredPropertiesSet();
		this.watchDir = configProperties.getPropertyAsFile(EtfConstants.ETF_PROJECTS_DIR);
		try {
			this.watchDir.expectDirIsReadable();
		} catch (IOException e) {
			throw new InitializationException(e);
		}
	}

	@Override
	protected void doBeforeDeregister(final Dto dto) {
		// nothing to do here
	}

	@Override
	protected void doAfterRegister(final Collection<Dto> dtos) {
		// nothing to do here
	}

	@Override
	protected void doRelease() {
		// nothing to do here
	}

	@Override protected void doBeforeVisit(final Set<Path> dirs) {
		// nothing to do here
	}

	@Override
	public ConfigPropertyHolder getConfigurationProperties() {
		return this.configProperties;
	}
}
