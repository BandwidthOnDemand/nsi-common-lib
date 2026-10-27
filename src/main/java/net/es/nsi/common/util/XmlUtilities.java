package net.es.nsi.common.util;

import java.io.File;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import net.es.nsi.common.jaxb.DomParser;
import javax.xml.transform.dom.DOMSource;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.CopyOnWriteArrayList;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlRootElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Legacy convenience methods for JAXB objects, XML dates, and XML filenames.
 *
 * <p>JAXB operations create a context per invocation. Object serialization uses the
 * synthetic root {@code {uri}local}; JAXBElement overloads preserve the supplied name.
 * Serialization returns null on JAXB failures, while parsing reports checked exceptions.
 * For repeated operations with a fixed binding context, use
 * {@link net.es.nsi.common.jaxb.JaxbParser}.
 *
 * @author hacksaw
 */
public class XmlUtilities {

  /**
   * Milliseconds in a fixed 365-day interval, independent of leap years.
   */
  public final static long ONE_YEAR = 31536000000L;
  /**
   * Milliseconds in a fixed 24-hour interval, independent of daylight-saving changes.
   */
  public final static long ONE_DAY = 86400000L;

  /**
   * Serializes without requesting indentation using the synthetic root {@code {uri}local}.
   *
   * @param xmlClass JAXB-bound value class
   * @param xmlObject value to serialize
   * @return XML text, or null for a null argument or JAXB failure
   */
  public static String jaxbToString(Class<?> xmlClass, Object xmlObject) {

    if (xmlClass == null || xmlObject == null) {
      return null;
    }

    @SuppressWarnings("unchecked")
    JAXBElement<?> jaxbElement = new JAXBElement(new QName("uri", "local"), xmlClass, xmlObject);

    return jaxbToString(xmlClass, jaxbElement);
  }

  /**
   * Serializes without requesting indentation using the supplied element name.
   *
   * @param xmlClass JAXB-bound value class
   * @param jaxbElement named JAXB element to serialize
   * @return XML text, or null for a null argument or JAXB failure
   */
  public static String jaxbToString(Class<?> xmlClass, JAXBElement<?> jaxbElement) {
    return jaxbToString(false, xmlClass, jaxbElement);
  }

  /**
   * Serializes with indentation using the synthetic root {@code {uri}local}.
   * Formatting introduces whitespace and should occur before signing XML.
   *
   * @param xmlClass JAXB-bound value class
   * @param xmlObject value to serialize
   * @return XML text, or null for a null argument or JAXB failure
   */
  public static String prettyPrint(Class<?> xmlClass, Object xmlObject) {

    if (xmlClass == null || xmlObject == null) {
      return null;
    }

    @SuppressWarnings("unchecked")
    JAXBElement<?> jaxbElement = new JAXBElement(new QName("uri", "local"), xmlClass, xmlObject);

    return jaxbToString(true, xmlClass, jaxbElement);
  }

  /**
   * Serializes with indentation using the supplied element name.
   * Formatting introduces whitespace and should occur before signing XML.
   *
   * @param xmlClass JAXB-bound value class
   * @param jaxbElement named JAXB element to serialize
   * @return XML text, or null for a null argument or JAXB failure
   */
  public static String prettyPrint(Class<?> xmlClass, JAXBElement<?> jaxbElement) {
    return jaxbToString(true, xmlClass, jaxbElement);
  }

  /**
   * Marshals a named element with a new class-based JAXB context.
   *
   * @param pretty whether to request formatted output
   * @param xmlClass JAXB-bound value class
   * @param jaxbElement named element to serialize
   * @return XML text, or null for a null class, null element, or JAXB failure
   */
  public static String jaxbToString(boolean pretty, Class<?> xmlClass, JAXBElement<?> jaxbElement) {
    if (xmlClass == null || jaxbElement == null) {
      return null;
    }

    StringWriter writer = new StringWriter();
    String result;
    try {
      final JAXBContext jaxbContext = JAXBContext.newInstance(xmlClass);

      Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
      jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, pretty);
      jaxbMarshaller.marshal(jaxbElement, writer);
      result = writer.toString();
    } catch (JAXBException e) {
      // Preserve the legacy null-on-JAXB-failure serialization contract.
      return null;
    } finally {
      try {
        writer.close();
      } catch (IOException ex) {
        // StringWriter owns no external resource; closure has no effect on its contents.
      }
    }

    return result;
  }

  /**
   * Parses XML characters securely and binds the resulting root to the requested class.
   *
   * @param <T> bound result type
   * @param xmlClass non-null JAXB-bound result class
   * @param xml non-null XML character content
   * @return the bound object, or null when the bound element value is null
   * @throws JAXBException if XML is malformed, prohibited, or incompatible with the requested class
   */
  public static <T> T xmlToJaxb(Class<T> xmlClass, String xml) throws JAXBException {
    try {
      return unmarshal(xmlClass, DomParser.xml2Dom(xml));
    } catch (IOException | ParserConfigurationException | SAXException ex) {
      throw new JAXBException("Could not parse XML", ex);
    }
  }

  /**
   * Parses XML bytes securely without closing the borrowed stream, then binds the root.
   *
   * @param <T> bound result type
   * @param xmlClass non-null JAXB-bound result class
   * @param is non-null XML byte stream; encoding follows the XML declaration
   * @return the bound object, or null when the bound element value is null
   * @throws JAXBException if XML is malformed, prohibited, or incompatible with the requested class
   * @throws IOException if the source stream cannot be read
   */
  public static <T> T xmlToJaxb(Class<T> xmlClass, InputStream is) throws JAXBException, IOException {
    try {
      return unmarshal(xmlClass, DomParser.xml2Dom(is));
    } catch (ParserConfigurationException | SAXException ex) {
      throw new JAXBException("Could not parse XML", ex);
    }
  }

  /**
   * Binds synthetic roots with the explicit declared type and checks normal roots for
   * type compatibility after unwrapping a JAXBElement.
   *
   * @param <T> bound result type
   * @param xmlClass expected bound class
   * @param document securely parsed DOM
   * @return the bound object, or null when the bound element value is null
   * @throws JAXBException if binding fails or the root maps to another type
   */
  private static <T> T unmarshal(Class<T> xmlClass, Document document) throws JAXBException {
    Unmarshaller reader = JAXBContext.newInstance(xmlClass).createUnmarshaller();
    Element root = document.getDocumentElement();
    // The generic serializers intentionally wrap unrooted values in {uri}local.
    if ("uri".equals(root.getNamespaceURI()) && "local".equals(root.getLocalName())) {
      return reader.unmarshal(new DOMSource(document), xmlClass).getValue();
    }
    Object value = reader.unmarshal(document);
    if (value instanceof JAXBElement<?> element) {
      value = element.getValue();
    }
    if (value != null && !xmlClass.isInstance(value)) {
      throw new JAXBException("Expected XML for " + xmlClass.getName() + " but found " + value.getClass().getName());
    }
    return xmlClass.cast(value);
  }

  /**
   * Converts nonnegative milliseconds since the Unix epoch using the default time zone.
   *
   * @param time milliseconds since 1970-01-01T00:00:00Z
   * @return an XML calendar representing the instant
   * @throws DatatypeConfigurationException if time is negative or no XML datatype factory is available
   */
  public static XMLGregorianCalendar longToXMLGregorianCalendar(long time) throws DatatypeConfigurationException {
    if (time < 0) {
      throw new DatatypeConfigurationException("Illegal time value specified " + time);
    }

    GregorianCalendar cal = new GregorianCalendar();
    cal.setTimeInMillis(time);
    XMLGregorianCalendar newXMLGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
    return newXMLGregorianCalendar;
  }

  /**
   * Captures the current time using the JVM default time zone.
   *
   * @return an XML calendar for the current instant
   * @throws DatatypeConfigurationException if no XML datatype factory is available
   */
  public static XMLGregorianCalendar xmlGregorianCalendar() throws DatatypeConfigurationException {
    GregorianCalendar cal = new GregorianCalendar();
    return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
  }

  /**
   * Converts a Date using the JVM default time zone.
   *
   * @param date non-null instant to convert
   * @return an XML calendar representing the same instant
   * @throws DatatypeConfigurationException if no XML datatype factory is available
   */
  public static XMLGregorianCalendar xmlGregorianCalendar(Date date) throws DatatypeConfigurationException {
    GregorianCalendar cal = new GregorianCalendar();
    cal.setTime(date);
    return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
  }

  /**
   * Parses an ISO date/time and represents the instant in the JVM default time zone.
   * A missing input offset is interpreted by the ISO parser using its default zone.
   *
   * @param date ISO date/time text accepted by the Joda-Time ISO parser
   * @return an XML calendar representing the parsed instant
   * @throws DatatypeConfigurationException if no XML datatype factory is available
   * @throws IllegalArgumentException if the date text cannot be parsed
   */
  public static XMLGregorianCalendar xmlGregorianCalendar(String date) throws DatatypeConfigurationException {
    DateTime dt = ISODateTimeFormat.dateTimeParser().parseDateTime(date);
    GregorianCalendar cal = new GregorianCalendar();
    cal.setTime(dt.toDate());
    return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
  }

  /**
   * Converts an XML calendar through its Gregorian-calendar representation.
   * Undefined XML fields use the defaults supplied by XMLGregorianCalendar.
   *
   * @param cal non-null XML calendar
   * @return the corresponding Date
   * @throws DatatypeConfigurationException retained in the signature for compatibility; this implementation does not create a datatype factory
   */
  public static Date xmlGregorianCalendarToDate(XMLGregorianCalendar cal) throws DatatypeConfigurationException {
    GregorianCalendar gregorianCalendar = cal.toGregorianCalendar();
    return gregorianCalendar.getTime();
  }

  /**
   * Lists direct child files whose names end with lowercase {@code .xml}.
   * The search is nonrecursive and has no ordering guarantee. Missing, unreadable, or
   * non-directory paths produce an empty collection.
   *
   * @param path non-null directory path
   * @return a new mutable collection of absolute filenames
   * @throws NullPointerException if the path is null
   */
  public static Collection<String> getXmlFilenames(String path) throws NullPointerException {
    Collection<String> results = new CopyOnWriteArrayList<>();
    File folder = new File(path);

    File[] listOfFiles = folder.listFiles();
    if (listOfFiles != null) {
      String file;
      for (int i = 0; i < listOfFiles.length; i++) {
        if (listOfFiles[i].isFile()) {
          file = listOfFiles[i].getAbsolutePath();
          if (file.endsWith(".xml") || file.endsWith(".xml")) {
            results.add(file);
          }
        }
      }
    }
    return results;
  }
}
