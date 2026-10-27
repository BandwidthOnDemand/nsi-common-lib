package net.es.nsi.common.util;

import java.util.Date;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Converts between Java instants and XML Gregorian calendars.
 *
 * <p>New XML calendars use the JVM default time zone. String parsing preserves the
 * represented instant but does not retain the source offset in the returned calendar.
 *
 * @author hacksaw
 */
public class XmlDate {

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
}
