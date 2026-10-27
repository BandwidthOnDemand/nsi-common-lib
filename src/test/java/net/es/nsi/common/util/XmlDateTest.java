package net.es.nsi.common.util;

import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;
import javax.xml.datatype.DatatypeConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests both date utility APIs while serializing changes to the JVM default time zone.
 */
@ResourceLock("default-time-zone")
class XmlDateTest {
    /**
     * Leap-day instant with fractional seconds for detecting lossy conversions.
     */
    private static final Instant INSTANT = Instant.parse("2024-02-29T12:34:56.789Z");

    /**
     * Checks epoch, Date, parsed offsets, and current-time conversions across default zones.
     *
     * @param utility true to exercise XmlUtilities; false to exercise XmlDate
     * @param zone temporary JVM default time-zone ID
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @ParameterizedTest
    @CsvSource({"true,UTC", "false,UTC", "true,America/New_York", "false,Asia/Tokyo"})
    void conversionsPreserveTheInstantAcrossTimeZones(boolean utility, String zone) throws Exception {
        TimeZone saved = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(zone));
            var xml = utility ? XmlUtilities.longToXMLGregorianCalendar(INSTANT.toEpochMilli())
                    : XmlDate.longToXMLGregorianCalendar(INSTANT.toEpochMilli());
            assertEquals(INSTANT, xml.toGregorianCalendar().toInstant());
            xml = utility ? XmlUtilities.xmlGregorianCalendar(Date.from(INSTANT)) : XmlDate.xmlGregorianCalendar(Date.from(INSTANT));
            assertEquals(Date.from(INSTANT), utility ? XmlUtilities.xmlGregorianCalendarToDate(xml) : XmlDate.xmlGregorianCalendarToDate(xml));
            xml = utility ? XmlUtilities.xmlGregorianCalendar("2024-02-29T08:34:56.789-04:00")
                    : XmlDate.xmlGregorianCalendar("2024-02-29T08:34:56.789-04:00");
            assertEquals(INSTANT, xml.toGregorianCalendar().toInstant());
            long before = System.currentTimeMillis();
            xml = utility ? XmlUtilities.xmlGregorianCalendar() : XmlDate.xmlGregorianCalendar();
            long after = System.currentTimeMillis();
            assertTrue(xml.toGregorianCalendar().getTimeInMillis() >= before);
            assertTrue(xml.toGregorianCalendar().getTimeInMillis() <= after);
        } finally {
            // Default time zone is process-wide and must be restored even after an assertion fails.
            TimeZone.setDefault(saved);
        }
    }

    /**
     * Rejects nonexistent calendar dates and malformed date text in both utility APIs.
     *
     * @param value invalid ISO date/time text
     */
    @ParameterizedTest
    @ValueSource(strings = {"2023-02-29T12:00:00Z", "not-a-date", "2024-13-01T00:00:00Z"})
    void invalidDatesFailExplicitly(String value) {
        assertThrows(IllegalArgumentException.class, () -> XmlDate.xmlGregorianCalendar(value));
        assertThrows(IllegalArgumentException.class, () -> XmlUtilities.xmlGregorianCalendar(value));
    }

    /**
     * Checks the documented nonnegative epoch-millisecond boundary.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void epochIsAcceptedAndNegativeEpochIsRejected() throws Exception {
        assertEquals(Instant.EPOCH, XmlDate.longToXMLGregorianCalendar(0).toGregorianCalendar().toInstant());
        assertThrows(DatatypeConfigurationException.class, () -> XmlDate.longToXMLGregorianCalendar(-1));
        assertThrows(DatatypeConfigurationException.class, () -> XmlUtilities.longToXMLGregorianCalendar(-1));
    }
}
