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
package de.interactive_instruments.etf;

import static de.interactive_instruments.etf.EtfConstants.ETF_XMLNS;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import de.interactive_instruments.SUtils;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.etf.model.EidFactory;

/**
 *
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class EtfXpathEvaluator {
    private EtfXpathEvaluator() {}

    public static XPath newXPath() {
        final XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new NamespaceContext() {
            public String getNamespaceURI(String prefix) {
                if (prefix == null)
                    throw new NullPointerException("Null prefix");
                else if ("etf".equals(prefix))
                    return ETF_XMLNS;
                else if ("etfAppinfo".equals(prefix))
                    return "http://www.interactive-instruments.de/etf/appinfo/1.0";
                return XMLConstants.NULL_NS_URI;
            }

            public String getPrefix(String uri) {
                throw new UnsupportedOperationException();
            }

            public Iterator getPrefixes(String uri) {
                throw new UnsupportedOperationException();
            }
        });
        return xpath;
    }

    public static EID evalEid(final String expr, final File file) throws IOException, XPathExpressionException {
        final XPath xpath = EtfXpathEvaluator.newXPath();
        final de.interactive_instruments.XmlUtils.XmlHandle xmlHandle = de.interactive_instruments.XmlUtils.newXmlHandle(xpath,
                file);
        final String oid = xmlHandle.evaluateValue(expr);
        if (SUtils.isNullOrEmpty(oid)) {
            throw new IOException("EID not set in '" + file + "'");
        }
        if (oid.length() != 39) {
            throw new IOException("EID '" + oid + "' in file '" + file + "' is invalid");
        }
        return EidFactory.getDefault().createUUID(oid.substring(3));
    }

    public static EID evalEidOrNull(final String expr, final File file) throws IOException, XPathExpressionException {
        final XPath xpath = EtfXpathEvaluator.newXPath();
        final de.interactive_instruments.XmlUtils.XmlHandle xmlHandle = de.interactive_instruments.XmlUtils.newXmlHandle(xpath,
                file);
        final String oid = xmlHandle.evaluateValue(expr);
        if (SUtils.isNullOrEmpty(oid)) {
            return null;
        }
        if (oid.length() != 39) {
            throw new IOException("EID '" + oid + "' in file '" + file + "' is invalid");
        }
        return EidFactory.getDefault().createUUID(oid.substring(3));
    }

    public static Collection<EID> evalEids(final String expr, final File file) throws IOException, XPathExpressionException {
        final XPath xpath = EtfXpathEvaluator.newXPath();
        final de.interactive_instruments.XmlUtils.XmlHandle xmlHandle = de.interactive_instruments.XmlUtils.newXmlHandle(xpath,
                file);
        final String[] values = xmlHandle.evaluateValues(expr);
        final List<EID> eids = new ArrayList<>(values.length);
        if (values != null) {
            for (final String oid : values) {
                if (oid.length() != 39) {
                    throw new IOException("EID '" + oid + "' in file '" + file + "' is invalid");
                }
                eids.add(EidFactory.getDefault().createUUID(oid));
            }
        }
        return eids;
    }
}
