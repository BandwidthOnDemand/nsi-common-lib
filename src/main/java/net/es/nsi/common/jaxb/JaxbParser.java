package net.es.nsi.common.jaxb;

import java.io.BufferedInputStream;
import org.xml.sax.SAXException;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Binds XML to generated JAXB models using one cached context per parser instance.
 *
 * <p>Each operation creates its own marshaller or unmarshaller. Binding state can be
 * reused across threads; callers must coordinate access to shared files and mutable
 * model objects. String input is already decoded text; stream input honors the XML
 * encoding declaration. Parsed input rejects DTDs and leaves borrowed streams open.
 *
 * <p>The generated root must be represented by a {@link JAXBElement}; typed parsing
 * also verifies the root's declared Java type.
 *
 * @author hacksaw
 */
public class JaxbParser {
  /**
   * Logger for JAXB serialization diagnostics.
   */
  private final static Logger LOG = LogManager.getLogger(JaxbParser.class);
  /**
   * Cached binding metadata; mutable marshaller and unmarshaller instances are never shared.
   */
  private final JAXBContext jc;

  /**
   * Initializes binding metadata for one or more colon-separated packages.
   *
   * @param packages JAXB context path containing generated binding packages
   * @throws IllegalArgumentException if JAXB cannot initialize the requested context
   */
  public JaxbParser(String packages) {
    try {
      jc = JAXBContext.newInstance(packages);
    } catch (JAXBException jaxb) {
      throw new IllegalArgumentException("Cannot initialize JAXB context for packages " + packages, jaxb);
    }
  }

  /**
   * Creates a per-operation marshaller with formatted output enabled.
   *
   * @return a new marshaller
   * @throws JAXBException if the provider cannot create or configure the marshaller
   */
  private Marshaller marshaller() throws JAXBException {
    Marshaller marshaller = jc.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
    return marshaller;
  }

  /**
   * Creates a per-operation unmarshaller so parsing state is never shared.
   *
   * @return a new unmarshaller
   * @throws JAXBException if the provider cannot create the unmarshaller
   */
  private Unmarshaller unmarshaller() throws JAXBException {
    return jc.createUnmarshaller();
  }

  /**
   * Reads a model from a file and closes the streams opened for that file.
   *
   * @param <T> expected root value type
   * @param xmlClass expected generated root class
   * @param file path of the XML input file
   * @return the parsed root value
   * @throws JAXBException if XML, the root namespace, or the declared root type is invalid
   * @throws IOException if the file cannot be opened or read
   */
  public <T extends Object> T parseFile(Class<T> xmlClass, String file)
          throws JAXBException, IOException {
    try (FileInputStream fileInputStream = new FileInputStream(file);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {
      return xml2Jaxb(xmlClass, bufferedInputStream);
    }
  }

  /**
   * Writes formatted XML, creating missing parent directories and replacing existing content.
   *
   * <p>Streams opened for the file are closed before this method returns.
   *
   * @param jaxbElement root element containing the model to write
   * @param file destination path
   * @throws JAXBException if the model cannot be marshalled
   * @throws IOException if the destination cannot be created or written
   */
  public void writeFile(JAXBElement<?> jaxbElement, String file) throws JAXBException, IOException {
    File fd = new File(file);
    if (!fd.exists()) {
      LOG.debug("Creating file " + fd.getAbsolutePath());
      FileUtils.touch(fd);
    }

    // This method owns both output wrappers and closes them even if marshalling fails.
    try (FileOutputStream fileOutputStream = new FileOutputStream(file);
            BufferedOutputStream bufferedOutputStream =
                    new BufferedOutputStream(fileOutputStream)) {
      marshaller().marshal(jaxbElement, bufferedOutputStream);
    }
  }

  /**
   * Marshals a root element into a new namespace-aware DOM document.
   *
   * @param jaxbElement root element and value to serialize
   * @return a new document containing the serialized model
   * @throws JAXBException if marshalling fails
   * @throws ParserConfigurationException if a document builder cannot be created
   */
  public Document jaxb2Dom(JAXBElement<?> jaxbElement) throws JAXBException, ParserConfigurationException {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.newDocument();
    marshaller().marshal(jaxbElement, doc);
    return doc;
  }

  /**
   * Unmarshals an existing DOM document using this parser context.
   *
   * <p>The caller is responsible for how the DOM was originally parsed.
   *
   * @param doc non-null document with a root mapped as a JAXB element
   * @return the unmarshalled root element
   * @throws JAXBException if the document cannot be bound to this context
   * @throws ClassCastException if the bound root is not represented by a JAXB element
   */
  public JAXBElement<?> dom2Jaxb(Document doc) throws JAXBException {
    return (JAXBElement<?>) unmarshaller().unmarshal(doc);
  }

  /**
   * Unmarshals a DOM element or document node using this parser context.
   *
   * @param node non-null node with a root mapped as a JAXB element
   * @return the unmarshalled root element
   * @throws JAXBException if the node cannot be bound to this context
   * @throws ClassCastException if the bound root is not represented by a JAXB element
   */
  public JAXBElement<?> node2Jaxb(Node node) throws JAXBException {
    return (JAXBElement<?>) unmarshaller().unmarshal(node);
  }

  /**
   * Serializes a JAXB root element to formatted XML characters.
   *
   * @param jaxbElement root element and value to serialize
   * @return XML text with a UTF-8 declaration
   * @throws JAXBException if the model cannot be marshalled
   */
  public String jaxb2Xml(JAXBElement<?> jaxbElement) throws JAXBException {
    // A character writer preserves text independently of any platform-default charset.
    StringWriter writer = new StringWriter();
    String result;
    try {
      marshaller().marshal(jaxbElement, writer);
      result = writer.toString();
    } catch (JAXBException ex) {
      // Preserve the binding failure for callers while recording serialization context.
      LOG.error("Failed to serialize JAXB structure.", ex);
      throw ex;
    } finally {
      try {
        writer.close();
      } catch (IOException ex) {
        // StringWriter owns no external resource and remains readable after closure.
      }
    }

    return result;
  }

  /**
   * Serializes a JAXB root element to formatted XML characters.
   *
   * @param jaxbElement root element and value to serialize
   * @return XML text with a UTF-8 declaration
   * @throws JAXBException if the model cannot be marshalled
   */
  public String jaxb2XmlFormatter(JAXBElement<?> jaxbElement) throws JAXBException {
    // A character writer preserves text independently of any platform-default charset.
    StringWriter writer = new StringWriter();
    String result;
    try {
      Marshaller marshaller = marshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      marshaller.marshal(jaxbElement, writer);
      result = writer.toString();
    } catch (JAXBException ex) {
      // Preserve the binding failure for callers while recording serialization context.
      LOG.error("Failed to serialize JAXB structure.", ex);
      throw ex;
    } finally {
      try {
        writer.close();
      } catch (IOException ex) {
        // StringWriter owns no external resource and remains readable after closure.
      }
    }

    return result;
  }

  /**
   * Binds already-decoded XML characters to the exact expected root type.
   *
   * <p>An encoding declaration does not reinterpret Java string characters.
   *
   * @param <T> expected root value type
   * @param xmlClass expected generated root class
   * @param xml non-null XML character input
   * @return the parsed root value
   * @throws JAXBException if secure XML parsing, root binding, or type matching fails
   */
  public <T extends Object> T xml2Jaxb(Class<T> xmlClass, String xml) throws JAXBException, IllegalArgumentException {
    try {
      return unmarshal(xmlClass, DomParser.xml2Dom(xml));
    } catch (IOException | ParserConfigurationException | SAXException ex) {
      throw new JAXBException("Could not parse XML", ex);
    }
  }

  /**
   * Binds XML bytes while preserving caller ownership of the input stream.
   *
   * @param <T> expected root value type
   * @param xmlClass expected generated root class
   * @param is non-null borrowed stream whose encoding follows its XML declaration
   * @return the parsed root value
   * @throws JAXBException if secure XML parsing, root binding, or type matching fails
   * @throws IOException if the stream cannot be read
   */
  public <T extends Object> T xml2Jaxb(Class<T> xmlClass, InputStream is) throws JAXBException, IOException {
    try {
      return unmarshal(xmlClass, DomParser.xml2Dom(is));
    } catch (ParserConfigurationException | SAXException ex) {
      throw new JAXBException("Could not parse XML", ex);
    }
  }

  /**
   * Binds XML bytes while preserving caller ownership of the input stream.
   *
   * @param <T> expected root value type
   * @param xmlClass expected generated root class
   * @param is non-null borrowed stream whose encoding follows its XML declaration
   * @return the parsed root value
   * @throws JAXBException if secure XML parsing, root binding, or type matching fails
   * @throws IOException if the stream cannot be read
   */
  public <T extends Object> T xml2Jaxb(Class<T> xmlClass, BufferedInputStream is) throws JAXBException, IOException {
    return xml2Jaxb(xmlClass, (InputStream) is);
  }

  /**
   * Checks the root wrapper and declared type after context-based unmarshalling.
   *
   * @param <T> expected root value type
   * @param xmlClass expected generated root class
   * @param document already parsed namespace-aware document
   * @return the matching root value
   * @throws JAXBException if binding or the root wrapper/type contract fails
   */
  private <T> T unmarshal(Class<T> xmlClass, Document document) throws JAXBException {
    Object value = unmarshaller().unmarshal(document);
    if (value instanceof JAXBElement<?> element) {
      if (element.getDeclaredType() == xmlClass) {
        return xmlClass.cast(element.getValue());
      }
      throw new JAXBException("Expected XML for " + xmlClass.getName() + " but found "
              + element.getDeclaredType().getName());
    }
    throw new JAXBException("Expected a JAXB element for " + xmlClass.getName());
  }

  /**
   * Serializes an arbitrary bound value beneath the synthetic root {@code {uri}local}.
   *
   * <p>A fresh class-based context is used for this standalone convenience method.
   * JAXB failures are logged and returned as null.
   *
   * @param <T> declared value type
   * @param messageClass bound value class, or null
   * @param message value assignable to that class, or null
   * @return XML text, or null for absent arguments or a JAXB failure
   * @throws ClassCastException if the value is incompatible with the declared class
   */
  public static <T extends Object> String jaxb2String(Class<T> messageClass, Object message) {

    if (messageClass == null || message == null) {
      return null;
    }

    // A character writer preserves text independently of any platform-default charset.
    StringWriter writer = new StringWriter();

    try {
      // An explicit root also supports generated values without @XmlRootElement.
      JAXBElement<T> element = new JAXBElement<T>(new QName("uri", "local"), messageClass, messageClass.cast(message));

      final JAXBContext jaxbContext = JAXBContext.newInstance(messageClass);
      jaxbContext.createMarshaller().marshal(element, writer);
    } catch (JAXBException e) {
      // Preserve the legacy null result while retaining diagnostics in the log.
      LOG.error("[JaxbParser] jaxb2String: Error marshalling object "
              + messageClass.getName() + ": " + e.toString());
      return null;
    }

    return writer.toString();
  }
}
