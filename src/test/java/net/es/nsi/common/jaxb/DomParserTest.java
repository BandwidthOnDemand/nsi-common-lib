package net.es.nsi.common.jaxb;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests namespace-aware DOM conversion, character encoding, and rejection of unsafe XML.
 */
class DomParserTest {
    /**
     * JUnit-managed directory for external-entity and DTD fixtures.
     */
    @TempDir Path directory;

    /**
     * Checks Unicode and namespaces while preserving borrowed-stream and DOM ownership.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void preservesNamespacesUnicodeAndCallerOwnedStreams() throws Exception {
        String xml = "<p:root xmlns:p='urn:test'><p:value>café ☃</p:value></p:root>";
        /**
         * Records close calls to detect accidental ownership of a borrowed stream.
         */
        class TrackingStream extends ByteArrayInputStream {
            /**
             * Whether the parser attempted to close the supplied stream.
             */
            boolean closed;
            /**
             * Creates a stream containing the XML fixture bytes.
             */
            TrackingStream() { super(xml.getBytes(StandardCharsets.UTF_8)); }
            /**
             * Records closure without discarding the in-memory fixture.
             */
            @Override public void close() { closed = true; }
        }
        TrackingStream source = new TrackingStream();
        Document doc = DomParser.xml2Dom(source);
        assertFalse(source.closed);
        assertEquals("urn:test", doc.getDocumentElement().getNamespaceURI());
        assertEquals("café ☃", doc.getDocumentElement().getTextContent());
        boolean standalone = doc.getXmlStandalone();
        String serialized = DomParser.doc2Xml(doc);
        assertEquals(standalone, doc.getXmlStandalone());
        assertEquals("café ☃", DomParser.xml2Dom(serialized).getDocumentElement().getTextContent());
        assertEquals("root", DomParser.xml2Dom(DomParser.prettyPrint(doc)).getDocumentElement().getLocalName());
    }

    /**
     * Rejects empty, non-XML, and structurally broken inputs.
     *
     * @param xml malformed XML character input
     */
    @ParameterizedTest
    @ValueSource(strings = {"", "<root>", "<root><a></root>", "plain text"})
    void rejectsMalformedXml(String xml) {
        assertThrows(SAXException.class, () -> DomParser.xml2Dom(xml));
    }

    /**
     * Uses a local sentinel and a missing DTD to verify both external mechanisms are rejected.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void rejectsExternalEntitiesAndExternalDtds() throws Exception {
        Path secret = directory.resolve("secret.txt");
        Files.writeString(secret, "private sentinel");
        assertThrows(SAXException.class, () -> DomParser.xml2Dom("<!DOCTYPE root [<!ENTITY secret SYSTEM '"
                + secret.toUri() + "'>]><root>&secret;</root>"));
        assertThrows(SAXException.class, () -> DomParser.xml2Dom("<!DOCTYPE root SYSTEM '"
                + directory.resolve("missing.dtd").toUri() + "'><root/>"));
    }

    /**
     * Checks the distinction between decoded characters and declared byte encodings, including serialization.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void characterInputIsNotReinterpretedUsingItsEncodingDeclaration() throws Exception {
        String xml = "<?xml version='1.0' encoding='ISO-8859-1'?><root>café</root>";
        assertEquals("café", DomParser.xml2Dom(xml).getDocumentElement().getTextContent());
        Document fromBytes = DomParser.xml2Dom(new ByteArrayInputStream(xml.getBytes(StandardCharsets.ISO_8859_1)));
        assertEquals("café", fromBytes.getDocumentElement().getTextContent());
        assertEquals("café", DomParser.xml2Dom(DomParser.doc2Xml(fromBytes)).getDocumentElement().getTextContent());
        assertEquals("café", DomParser.xml2Dom(DomParser.prettyPrint(fromBytes)).getDocumentElement().getTextContent());
    }
}
