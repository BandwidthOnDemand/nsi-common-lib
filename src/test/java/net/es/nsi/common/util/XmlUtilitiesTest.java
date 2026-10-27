package net.es.nsi.common.util;

import jakarta.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import net.es.nsi.common.jaxb.nsa.NsaType;
import net.es.nsi.common.jaxb.nsa.ObjectFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests legacy JAXB convenience contracts and nonrecursive XML filename discovery.
 */
class XmlUtilitiesTest {
    /**
     * JUnit-managed directory for filename-discovery fixtures.
     */
    @TempDir Path directory;

    /**
     * Checks synthetic and schema roots, formatting, Unicode, and null serialization inputs.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void genericAndNamedElementSerializationsRoundTrip() throws Exception {
        NsaType nsa = new NsaType(); nsa.setId("urn:test:nsa"); nsa.setName("café");
        var element = new ObjectFactory().createNsa(nsa);
        for (String xml : new String[]{XmlUtilities.jaxbToString(NsaType.class, nsa),
                XmlUtilities.prettyPrint(NsaType.class, nsa), XmlUtilities.jaxbToString(NsaType.class, element),
                XmlUtilities.prettyPrint(NsaType.class, element)}) {
            assertNotNull(xml);
            assertEquals(nsa.getId(), XmlUtilities.xmlToJaxb(NsaType.class, xml).getId());
            try (var stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
                assertEquals(nsa.getName(), XmlUtilities.xmlToJaxb(NsaType.class, stream).getName());
            }
        }
        assertNull(XmlUtilities.jaxbToString(null, nsa));
        assertNull(XmlUtilities.prettyPrint(null, nsa));
        assertNull(XmlUtilities.jaxbToString(NsaType.class, (Object) null));
        assertNull(XmlUtilities.prettyPrint(NsaType.class, (Object) null));
    }

    /**
     * Preserves a null value when parsing a serialized nil named root from characters.
     *
     * @throws Exception if fixture preparation or parsing fails unexpectedly
     */
    @Test
    void nilNamedElementReturnsNullFromString() throws Exception {
        String xml = XmlUtilities.jaxbToString(NsaType.class, new ObjectFactory().createNsa(null));
        assertNotNull(xml);
        assertNull(XmlUtilities.xmlToJaxb(NsaType.class, xml));
    }

    /**
     * Preserves a null value and caller stream ownership when parsing a nil named root.
     *
     * @throws Exception if fixture preparation or parsing fails unexpectedly
     */
    @Test
    void nilNamedElementReturnsNullWithoutClosingStream() throws Exception {
        String xml = XmlUtilities.jaxbToString(NsaType.class, new ObjectFactory().createNsa(null));
        assertNotNull(xml);
        class TrackingStream extends ByteArrayInputStream {
            boolean closed;

            TrackingStream(byte[] bytes) {
                super(bytes);
            }

            @Override
            public void close() {
                closed = true;
            }
        }
        try (var stream = new TrackingStream(xml.getBytes(StandardCharsets.UTF_8))) {
            assertNull(XmlUtilities.xmlToJaxb(NsaType.class, stream));
            assertFalse(stream.closed, "Parsing must leave the caller-owned stream open");
        }
    }

    /**
     * Rejects a recognized root whose non-null bound value has an incompatible type.
     */
    @Test
    void incompatibleNamedElementStillThrowsJaxbException() {
        String xml = XmlUtilities.jaxbToString(NsaType.class, new ObjectFactory().createText("incompatible"));
        assertNotNull(xml);
        JAXBException exception = assertThrows(JAXBException.class,
                () -> XmlUtilities.xmlToJaxb(NsaType.class, xml));
        assertTrue(exception.getMessage().contains(String.class.getName()),
                "The known text root must reach the bound-value type check");
    }

    /**
     * Rejects malformed XML, DTD declarations, and roots outside the configured binding.
     */
    @Test
    void xmlUtilitiesRejectMalformedXmlAndDtds() {
        assertThrows(JAXBException.class, () -> XmlUtilities.xmlToJaxb(NsaType.class, "<bad>"));
        assertThrows(JAXBException.class, () -> XmlUtilities.xmlToJaxb(NsaType.class,
                "<!DOCTYPE nsa [<!ENTITY x 'unsafe'>]><nsa><name>&x;</name></nsa>"));
        assertThrows(JAXBException.class, () -> XmlUtilities.xmlToJaxb(NsaType.class, "<nsa xmlns='urn:wrong'/>"));
    }

    /**
     * Guards against treating an XML declaration as an instruction to re-encode Java text.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void xmlStringDeclarationsDoNotCorruptAlreadyDecodedCharacters() throws Exception {
        String xml = "<?xml version='1.0' encoding='ISO-8859-1'?>"
                + "<n:nsa xmlns:n='http://schemas.ogf.org/nsi/2014/02/discovery/nsa'><name>café</name></n:nsa>";
        assertEquals("café", XmlUtilities.xmlToJaxb(NsaType.class, xml).getName());
    }

    /**
     * Distinguishes regular XML files from other extensions, directories, and missing paths.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void listsOnlyXmlFilesAndHandlesMissingDirectories() throws Exception {
        Path xml = Files.writeString(directory.resolve("nsa.xml"), "<root/>");
        Files.writeString(directory.resolve("notes.txt"), "text");
        Files.createDirectory(directory.resolve("directory.xml"));
        assertEquals(Set.of(xml.toAbsolutePath().toString()), Set.copyOf(XmlUtilities.getXmlFilenames(directory.toString())));
        assertTrue(XmlUtilities.getXmlFilenames(directory.resolve("absent").toString()).isEmpty());
    }
}
