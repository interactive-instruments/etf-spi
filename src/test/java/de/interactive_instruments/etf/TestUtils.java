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
package de.interactive_instruments.etf;

import java.net.URI;
import java.util.Date;

import de.interactive_instruments.SUtils;
import de.interactive_instruments.etf.dal.dto.Dto;
import de.interactive_instruments.etf.dal.dto.MetaDataItemDto;
import de.interactive_instruments.etf.dal.dto.RepositoryItemDto;
import de.interactive_instruments.etf.dal.dto.capabilities.ComponentDto;
import de.interactive_instruments.etf.dal.dto.result.ResultModelItemDto;
import de.interactive_instruments.etf.dal.dto.result.TestResultStatus;
import de.interactive_instruments.etf.dal.dto.test.ExecutableTestSuiteDto;
import de.interactive_instruments.etf.model.EidFactory;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
class TestUtils {

	private TestUtils() {}

	static final ComponentDto COMP_DTO_1;

	static {
		COMP_DTO_1 = new ComponentDto();
		setBasicProperties(COMP_DTO_1, 1);
		COMP_DTO_1.setVendor("ii");
		COMP_DTO_1.setVersion("1.1.0");
	}

	static String toStrWithTrailingZeros(int i) {
		return String.format("%05d", i);
	}

	static String toStrWithTrailingZeros(long i) {
		return String.format("%05d", i);
	}

	static void setBasicProperties(final Dto dto, final long i) {
		final String name = dto.getClass().getSimpleName() + "." + toStrWithTrailingZeros(i);
		dto.setId(EidFactory.getDefault().createUUID(name));
		if (dto instanceof MetaDataItemDto) {
			final MetaDataItemDto mDto = ((MetaDataItemDto) dto);
			mDto.setLabel(name + ".label");
			mDto.setDescription(name + ".description");
		}
		if (dto instanceof RepositoryItemDto) {
			final RepositoryItemDto rDto = ((RepositoryItemDto) dto);
			rDto.setAuthor(name + ".author");
			rDto.setRemoteResource(URI.create("http://notset"));
			rDto.setLocalPath("/");
			rDto.setCreationDate(new Date(0));
			rDto.setVersionFromStr("1.0.0");
			rDto.setItemHash(SUtils.fastCalcHashAsHexStr(name));
		}
		if (dto instanceof ResultModelItemDto) {
			final ResultModelItemDto rDto = ((ResultModelItemDto) dto);
			rDto.setStartTimestamp(new Date(0));
			rDto.setResultStatus(TestResultStatus.FAILED);
			rDto.setDuration(1000);
		}
	}

	static ExecutableTestSuiteDto createEts(final int nr) {
		return createEts(nr, null);
	}

	static ExecutableTestSuiteDto createEts(final int nr, final ComponentDto testDriver) {
		final ExecutableTestSuiteDto ets = new ExecutableTestSuiteDto();
		ets.setLabel("ETS." + String.valueOf(nr));
		ets.setId(EidFactory.getDefault().createUUID(ets.getLabel()));
		ets.setTestDriver(testDriver);
		return ets;
	}
}
