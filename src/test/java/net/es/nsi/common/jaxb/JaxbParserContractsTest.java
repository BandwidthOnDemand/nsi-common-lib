package net.es.nsi.common.jaxb;

import jakarta.xml.bind.JAXBException;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.es.nsi.common.jaxb.nml.NmlTopologyType;
import net.es.nsi.common.jaxb.nsa.NsaType;
import net.es.nsi.common.jaxb.nsa.ObjectFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests secure JAXB parsing, encoding boundaries, stream ownership, and concurrent reuse.
 */
class JaxbParserContractsTest {
    /**
     * Namespace-qualified discovery XML containing a non-ASCII name.
     */
    private static final String XML = "<n:nsa xmlns:n='http://schemas.ogf.org/nsi/2014/02/discovery/nsa' id='urn:test:nsa'><name>café</name></n:nsa>";
    /**
     * JUnit-managed directory for input and nested output files.
     */
    @TempDir Path directory;

    /**
     * Checks strings, streams, buffered streams, and files against the same expected model.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void everyInputFormReturnsTheSameModelWithoutClosingBorrowedStreams() throws Exception {
        JaxbParser parser = NsaParser.getInstance();
        byte[] bytes = XML.getBytes(StandardCharsets.UTF_8);
        assertEquals("urn:test:nsa", parser.xml2Jaxb(NsaType.class, XML).getId());
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
            TrackingStream() { super(bytes); }
            /**
             * Records closure without discarding the in-memory fixture.
             */
            @Override public void close() { closed = true; }
        }
        TrackingStream source = new TrackingStream();
        assertEquals("urn:test:nsa", parser.xml2Jaxb(NsaType.class, source).getId());
        assertFalse(source.closed);
        try (BufferedInputStream buffered = new BufferedInputStream(new ByteArrayInputStream(bytes))) {
            assertEquals("urn:test:nsa", parser.xml2Jaxb(NsaType.class, buffered).getId());
        }
        Path file = directory.resolve("nsa.xml");
        Files.write(file, bytes);
        assertEquals("urn:test:nsa", parser.parseFile(NsaType.class, file.toString()).getId());
        assertThrows(IOException.class, () -> parser.parseFile(NsaType.class, directory.resolve("missing.xml").toString()));
    }

    /**
     * Rejects malformed XML, unknown roots, and forbidden DTD declarations.
     *
     * @param xml invalid or prohibited XML input
     */
    @ParameterizedTest
    @ValueSource(strings = {"<nsa>", "<nsa xmlns='urn:wrong'/>", "<!DOCTYPE nsa [<!ENTITY x 'value'>]><nsa xmlns='http://schemas.ogf.org/nsi/2014/02/discovery/nsa'><name>&x;</name></nsa>"})
    void rejectsMalformedUnknownAndDtdBearingXml(String xml) {
        assertThrows(JAXBException.class, () -> NsaParser.getInstance().xml2Jaxb(NsaType.class, xml));
    }

    /**
     * Checks fail-fast binding-context creation and mismatched requested result types.
     */
    @Test
    void contextAndRequestedTypeFailuresAreExplicit() {
        assertThrows(IllegalArgumentException.class, () -> new JaxbParser("no.such.binding.package"));
        assertThrows(JAXBException.class, () -> NsaParser.getInstance().xml2Jaxb(NmlTopologyType.class, XML));
    }

    /**
     * Guards against reinterpreting Java characters through an XML byte-encoding declaration.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void stringAndByteInputsRespectTheirDifferentEncodingContracts() throws Exception {
        String xml = "<?xml version='1.0' encoding='ISO-8859-1'?>" + XML;
        assertEquals("café", NsaParser.getInstance().xml2Jaxb(NsaType.class, xml).getName());
        assertEquals("café", NsaParser.getInstance().xml2Jaxb(NsaType.class,
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.ISO_8859_1))).getName());
    }

    /**
     * Checks all conversion forms and confirms nested output directories are created.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void domNodeStringAndFileConversionsPreserveContent() throws Exception {
        JaxbParser parser = NsaParser.getInstance();
        NsaType original = parser.xml2Jaxb(NsaType.class, XML);
        var element = new ObjectFactory().createNsa(original);
        var dom = parser.jaxb2Dom(element);
        assertEquals(original.getId(), ((NsaType) parser.dom2Jaxb(dom).getValue()).getId());
        assertEquals(original.getId(), ((NsaType) parser.node2Jaxb(dom.getDocumentElement()).getValue()).getId());
        assertEquals(original.getId(), parser.xml2Jaxb(NsaType.class, parser.jaxb2XmlFormatter(element)).getId());
        Path file = directory.resolve("nested").resolve("nsa.xml");
        parser.writeFile(element, file.toString());
        assertEquals(original.getId(), parser.parseFile(NsaType.class, file.toString()).getId());
        assertThrows(IOException.class, () -> parser.writeFile(element, directory.toString()));
        assertNotNull(JaxbParser.jaxb2String(NsaType.class, original));
        assertNull(JaxbParser.jaxb2String(null, original));
    }

    /**
     * Uses distinct identifiers across concurrent parses to detect shared unmarshaller state.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void concurrentSingletonParsingDoesNotShareUnmarshallerState() throws Exception {
        var executor = Executors.newFixedThreadPool(4);
        try {
            List<Callable<String>> tasks = java.util.stream.IntStream.range(0, 20)
                    .mapToObj(i -> (Callable<String>) () -> NsaParser.getInstance().xml2Jaxb(NsaType.class,
                            XML.replace("urn:test:nsa", "urn:test:nsa:" + i)).getId()).toList();
            var results = executor.invokeAll(tasks);
            for (int i = 0; i < results.size(); i++) {
                assertEquals("urn:test:nsa:" + i, results.get(i).get());
            }
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                    assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS),
                            "Concurrent parser workers did not terminate");
                }
            } catch (InterruptedException interrupted) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
                throw interrupted;
            }
        }
    }
}
