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
package de.interactive_instruments.etf.testdriver.detection;

import java.util.Collection;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.interactive_instruments.MediaType;
import de.interactive_instruments.etf.dal.dto.capabilities.TestObjectDto;
import de.interactive_instruments.etf.dal.dto.capabilities.TestObjectTypeDto;
import de.interactive_instruments.etf.model.EID;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class WfsTypeDetector implements XmlTypeDetector {

	@Override
	public XmlTypeDetectionCmd createCmd(final XMLReader xmlReader) {
		return null;
	}

	@Override
	public Collection<MediaType> supportedTypes() {
		return null;
	}

	@Override
	public boolean supportsDetectionByMimeType() {
		return true;
	}

	@Override
	public boolean supportsDetectionByFileExtension() {
		return true;
	}

	@Override
	public boolean supportsDetectionByContent() {
		return true;
	}

	@Override
	public EID getId() {
		return null;
	}

	private static class WfsTypeDetectorCmd implements XmlTypeDetectionCmd {

		private Status status = Status.NEED_MORE_DATA;
		// Wfs title
		private String label;
		// Wfs description
		private String description;
		int nodeLevel = 0;

		@Override
		public Status status() {
			return status;
		}

		@Override
		public void setType(final TestObjectDto dto) {

		}

		@Override
		public Collection<TestObjectTypeDto> getDetectibleTypes() {
			return null;
		}

		@Override
		public void setDocumentLocator(final Locator locator) {

		}

		@Override
		public void startDocument() throws SAXException {
			nodeLevel = 0;
			label = null;
			description = null;
		}

		@Override
		public void endDocument() throws SAXException {

		}

		@Override
		public void startPrefixMapping(final String prefix, final String uri) throws SAXException {

		}

		@Override
		public void endPrefixMapping(final String prefix) throws SAXException {

		}

		@Override
		public void startElement(final String uri, final String localName, final String qName, final Attributes atts)
				throws SAXException {

		}

		@Override
		public void endElement(final String uri, final String localName, final String qName) throws SAXException {

		}

		@Override
		public void characters(final char[] ch, final int start, final int length) throws SAXException {

		}

		@Override
		public void ignorableWhitespace(final char[] ch, final int start, final int length) throws SAXException {

		}

		@Override
		public void processingInstruction(final String target, final String data) throws SAXException {

		}

		@Override
		public void skippedEntity(final String name) throws SAXException {

		}
	}
}
