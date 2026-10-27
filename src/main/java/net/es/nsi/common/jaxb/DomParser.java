package net.es.nsi.common.jaxb;

import java.io.StringReader;
import org.xml.sax.InputSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.FilterInputStream;
import javax.xml.XMLConstants;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Parses XML securely and serializes DOM documents as UTF-8 XML text.
 *
 * <p>Parsing is namespace-aware, rejects DTDs and external entities, and does not
 * expand XInclude elements. Stream inputs stay open for the caller. Serialization
 * uses a cloned document so the caller's DOM and original encoding metadata are
 * not modified.
 *
 * @author hacksaw
 */
public class DomParser {

  /**
   * Parses bytes using their XML encoding declaration without closing the input.
   *
   * @param is non-null borrowed XML byte stream
   * @return a new namespace-aware document
   * @throws ParserConfigurationException if secure parser features cannot be configured
   * @throws SAXException if XML is malformed or contains a forbidden DTD
   * @throws IOException if input cannot be read
   */
  public static Document xml2Dom(InputStream is)
          throws ParserConfigurationException, SAXException, IOException {
    // Parsing must not close a stream owned by the caller.
    return builder().parse(new FilterInputStream(is) {
      /**
       * Leaves the borrowed stream open when the XML parser closes its wrapper.
       */
      @Override
      public void close() { }
    });
  }

  /**
   * Parses decoded XML characters without applying the declaration as a byte encoding.
   *
   * @param xml non-null XML character input
   * @return a new namespace-aware document
   * @throws ParserConfigurationException if secure parser features cannot be configured
   * @throws SAXException if XML is malformed or contains a forbidden DTD
   * @throws IOException if the parser cannot read its character input
   */
  public static Document xml2Dom(String xml)
          throws ParserConfigurationException, SAXException, IOException {
    try (StringReader reader = new StringReader(xml)) {
      return builder().parse(new InputSource(reader));
    }
  }

  /**
   * Creates an independent parser with external-resource processing disabled.
   *
   * @return a new configured document builder
   * @throws ParserConfigurationException if a required security feature is unavailable
   */
  private static DocumentBuilder builder() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);
    return factory.newDocumentBuilder();
  }

  /**
   * Serializes a clone of the document with a UTF-8 declaration.
   *
   * <p>Cloning drops parser byte-encoding metadata that the JDK identity transformer
   * otherwise prefers over the requested output encoding.
   *
   * @param doc non-null DOM document to serialize
   * @return XML characters with a UTF-8 declaration
   * @throws TransformerException if serializer creation or transformation fails
   * @throws TransformerConfigurationException if an identity transformer cannot be created
   */
  public static String doc2Xml(Document doc)
          throws TransformerException, TransformerConfigurationException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    TransformerFactory tf = TransformerFactory.newInstance();
    Transformer trans = tf.newTransformer();
    trans.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    // A clone drops the parser's original byte-encoding metadata, which the JDK identity
    // transformer otherwise uses in preference to the requested UTF-8 output encoding.
    trans.transform(new DOMSource(doc.cloneNode(true)), new StreamResult(baos));
    return new String(baos.toByteArray(), StandardCharsets.UTF_8);
  }

  /**
   * Serializes an indented clone of a DOM document as UTF-8 XML.
   *
   * <p>Indentation can change significant whitespace and invalidate an existing XML signature.
   *
   * @param document non-null document to format
   * @return indented XML characters
   * @throws TransformerConfigurationException if an identity transformer cannot be created
   * @throws TransformerException if the document cannot be transformed
   */
  public static String prettyPrint(Document document)
          throws TransformerConfigurationException, TransformerException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
    transformer.transform(new DOMSource(document.cloneNode(true)), new StreamResult(baos));
    return new String(baos.toByteArray(), StandardCharsets.UTF_8);
  }
}
