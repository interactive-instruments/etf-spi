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
package de.interactive_instruments.etf.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.interactive_instruments.etf.dal.dto.test.TestItemTypeDto;
import de.interactive_instruments.etf.dal.dto.translation.TranslationTemplateBundleDto;
import de.interactive_instruments.etf.dal.dto.translation.TranslationTemplateDto;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class StandardTypes {

	public static final EidMap<TestItemTypeDto> STANDARD_TEST_ITEM_TYPES = new DefaultEidMap<TestItemTypeDto>() {
		{
			{
				final TestItemTypeDto testItemTypeDto = new TestItemTypeDto();
				testItemTypeDto.setLabel("Manual Test Assertion");
				testItemTypeDto.setId(EidFactory.getDefault().createAndPreserveStr("b48eeaa3-6a74-414a-879c-1dc708017e11"));
				testItemTypeDto.setDescription("A Manual Test Assertion requires that a tester manually validates a result");
				testItemTypeDto.setReference(
						"https://github.com/interactive-instruments/etf-bsxtd/wiki/Test-Assertion-Types#test-assertion-type-2");
				put(testItemTypeDto.getId(), testItemTypeDto);
			}
			{
				final TestItemTypeDto testItemTypeDto = new TestItemTypeDto();
				testItemTypeDto.setLabel("Disabled Test Assertion");
				testItemTypeDto.setId(EidFactory.getDefault().createAndPreserveStr("92f22a19-2ec2-43f0-8971-c2da3eaafcd2"));
				testItemTypeDto.setDescription("");
				testItemTypeDto.setReference(
						"https://github.com/interactive-instruments/etf-bsxtd/wiki/Test-Assertion-Types#test-assertion-type-4");
				put(testItemTypeDto.getId(), testItemTypeDto);
			}

		}
	};

	public static final TranslationTemplateBundleDto STANDARD_TRANSLATION_TEMPLATE_BUNDLE = createTranslationTemplateBundle();

	private static TranslationTemplateBundleDto createTranslationTemplateBundle() {
		final TranslationTemplateBundleDto translationTemplateBundle = new TranslationTemplateBundleDto();
		translationTemplateBundle.setId(EidFactory.getDefault().createAndPreserveStr("4fde5176-9f9e-4bd3-a952-3b248d715a0f"));
		translationTemplateBundle.setSource(URI.create("library://etf-spi"));
		final List<TranslationTemplateDto> translationTemplateDtos = new ArrayList<TranslationTemplateDto>() {
			{
				final TranslationTemplateDto template1En = new TranslationTemplateDto(
						"TR.unspecifiedForwardedError", Locale.ENGLISH.toLanguageTag(),
						"{error}");
				final TranslationTemplateDto template1De = new TranslationTemplateDto(
						"TR.unspecifiedForwardedError", Locale.GERMAN.toLanguageTag(),
						"{error}");
				add(template1En);
				add(template1De);
			}
		};
		translationTemplateBundle.addTranslationTemplates(translationTemplateDtos);
		return translationTemplateBundle;
	}
}
