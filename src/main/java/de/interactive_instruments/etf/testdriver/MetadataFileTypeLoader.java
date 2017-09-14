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

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import de.interactive_instruments.etf.EtfConstants;
import de.interactive_instruments.etf.dal.dao.DataStorage;
import de.interactive_instruments.etf.dal.dto.Dto;
import de.interactive_instruments.etf.dal.dto.capabilities.TagDto;
import de.interactive_instruments.etf.dal.dto.capabilities.TestObjectTypeDto;
import de.interactive_instruments.etf.dal.dto.translation.TranslationTemplateBundleDto;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.InvalidStateTransitionException;
import de.interactive_instruments.exceptions.config.ConfigurationException;
import de.interactive_instruments.properties.ConfigProperties;
import de.interactive_instruments.properties.ConfigPropertyHolder;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public final class MetadataFileTypeLoader extends AbstractFileTypeLoader {

	private final ConfigProperties configProperties;

	public MetadataFileTypeLoader(final DataStorage dataStorageCallback) {
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
	public int fileChangeNotificationPriority() {
		return 200;
	}

	@Override
	protected void doBeforeDeregister(final Collection<? extends Dto> dtos) {
		// nothing to do here
	}

	@Override
	protected void doAfterRegister(final Collection<? extends Dto> dtos) {
		// nothing to do here
	}

	@Override
	protected void doRelease() {
		// nothing to do here
	}

	@Override
	protected void doBeforeVisit(final Set<Path> dirs) {
		// nothing to do here
	}

	@Override
	public ConfigPropertyHolder getConfigurationProperties() {
		return this.configProperties;
	}
}
