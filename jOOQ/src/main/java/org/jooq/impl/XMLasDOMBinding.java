/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Other licenses:
 * -----------------------------------------------------------------------------
 * Commercial licenses for this work are available. These replace the above
 * ASL 2.0 and offer limited warranties, support, maintenance, and commercial
 * database integrations.
 *
 * For more information, please visit: http://www.jooq.org/licenses
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package org.jooq.impl;

import static org.jooq.XML.xml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLXML;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.jooq.Converter;
import org.jooq.XML;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A binding that binds {@link Node} types to {@link SQLXML} types from your database.
 *
 * @author Lukas Eder
 */
public class XMLasDOMBinding extends AbstractXMLBinding<Node> {

    private static final Converter<XML, Node> CONVERTER = Converter.ofNullable(XML.class, Node.class,
        t -> XMLasDOMBinding.fromString(t.data()),
        u -> xml(XMLasDOMBinding.toString(u))
    );

    public XMLasDOMBinding() {}

    @Override
    public final Converter<XML, Node> converter() {
        return CONVERTER;
    }

    // ------------------------------------------------------------------------
    // The following logic originates from jOOX
    // ------------------------------------------------------------------------

    /**
     * Transform an {@link Node} into a <code>String</code>.
     */
    static final String toString(Node node) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            Source source = new DOMSource(node);
            Result target = new StreamResult(out);
            transformer.transform(source, target);
            return out.toString("UTF-8");
        }
        catch (Exception e) {
            return "[ ERROR IN toString() : " + e.getMessage() + " ]";
        }
    }

    /**
     * Create a new DOM element in an independent document
     */
    public static Document fromString(String name) {
        Document document = builder().newDocument();
        DocumentFragment fragment = createContent(document, name);

        if (fragment != null) {
            document.appendChild(fragment);
        }
        else {
            document.appendChild(document.createElement(name));
        }

        return document;
    }

    /**
     * Get a namespace-aware document builder
     */
    public static DocumentBuilder builder() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            // -----------------------------------------------------------------
            // [#4592] FIX START: Prevent OWASP attack vectors
            try {
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            }
            catch (ParserConfigurationException ignore) {}

            try {
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            }
            catch (ParserConfigurationException ignore) {}

            try {
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            }
            catch (ParserConfigurationException ignore) {}

            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            // [#4592] FIX END
            // -----------------------------------------------------------------

            // [#9] [#107] In order to take advantage of namespace-related DOM
            // features, the internal builder should be namespace-aware
            factory.setNamespaceAware(true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create some content in the context of a given document
     *
     * @return <ul>
     *         <li>A {@link DocumentFragment} if <code>text</code> is
     *         well-formed.</li>
     *         <li><code>null</code>, if <code>text</code> is plain text or not
     *         well formed</li>
     *         </ul>
     */
    static final DocumentFragment createContent(Document doc, String text) {

        // Text might hold XML content
        if (text != null && text.contains("<")) {
            DocumentBuilder builder = builder();

            try {

                // [#128] Trimming will get rid of leading and trailing whitespace, which would
                // otherwise cause a HIERARCHY_REQUEST_ERR raised by the parser
                text = text.trim();

                // There is a processing instruction. We can safely assume
                // valid XML and parse it as such
                if (text.startsWith("<?xml")) {
                    Document parsed = builder.parse(new InputSource(new StringReader(text)));
                    DocumentFragment fragment = parsed.createDocumentFragment();
                    fragment.appendChild(parsed.getDocumentElement());

                    return (DocumentFragment) doc.importNode(fragment, true);
                }

                // Any XML document fragment. To be on the safe side, fragments
                // are wrapped in a dummy root node
                else {
                    String wrapped = "<dummy>" + text + "</dummy>";
                    Document parsed = builder.parse(new InputSource(new StringReader(wrapped)));
                    DocumentFragment fragment = parsed.createDocumentFragment();
                    NodeList children = parsed.getDocumentElement().getChildNodes();

                    // appendChild removes children also from NodeList!
                    while (children.getLength() > 0) {
                        fragment.appendChild(children.item(0));
                    }

                    return (DocumentFragment) doc.importNode(fragment, true);
                }
            }

            // The XML content is invalid
            catch (IOException | SAXException ignore) {}
        }

        // Plain text or invalid XML
        return null;
    }
}
