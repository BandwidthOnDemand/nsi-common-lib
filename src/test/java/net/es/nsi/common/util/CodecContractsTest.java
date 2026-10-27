package net.es.nsi.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import net.es.nsi.common.jaxb.DomParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for encoding-layer order, UTF-8 fidelity, and explicit decoding failures.
 */
class CodecContractsTest {
    /**
     * Unicode and newline sentinel for detecting charset or MIME normalization differences.
     */
    private static final String TEXT = "café ☃\n";

    /**
     * Builds fixture data by applying content compression before transfer encoding.
     *
     * @param transfer MIME transfer encoding
     * @param type content type selecting compression
     * @param data unencoded bytes
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     * @return the fully encoded fixture text
     */
    private static String encoded(String transfer, String type, byte[] data) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (OutputStream output = ContentType.encode(type, ContentTransferEncoding.encode(transfer, bytes))) {
            output.write(data);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    /**
     * Checks consistent output across decoder overloads for each supported layer combination.
     *
     * @param transfer MIME transfer encoding
     * @param type content type selecting compression
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @ParameterizedTest
    @CsvSource({"base64,application/x-gzip", "base64,text/plain", "quoted-printable,text/plain", "7bit,text/plain"})
    void streamStringAndByteDecodingAgree(String transfer, String type) throws Exception {
        // MIME quoted-printable normalizes line endings; 7bit fixtures stay within ASCII.
        String text = transfer.equals("7bit") ? "ASCII text"
                : transfer.equals("quoted-printable") ? TEXT.replace("\n", "\r\n") : TEXT;
        byte[] source = text.getBytes(StandardCharsets.UTF_8);
        String encoded = encoded(transfer, type, source);
        assertEquals(text, Decoder.decode2string(transfer, type, encoded));
        assertArrayEquals(source, Decoder.decode2ByteArray(transfer, type, encoded));
        try (var decoded = Decoder.decode(transfer, type, encoded)) {
            assertArrayEquals(source, decoded.readAllBytes());
        }
    }

    /**
     * Checks the complete XML codec, namespace preservation, and absent-input defaults.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void encoderAndDecoderRoundTripUnicodeXml() throws Exception {
        var original = DomParser.xml2Dom("<root xmlns='urn:test'><value>" + TEXT + "</value></root>");
        String encoded = Encoder.encode(original);
        var decoded = Decoder.decode2dom("base64", ContentType.XGZIP, encoded);
        assertEquals(original.getDocumentElement().getNamespaceURI(), decoded.getDocumentElement().getNamespaceURI());
        assertEquals(original.getDocumentElement().getTextContent(), decoded.getDocumentElement().getTextContent());
        assertNull(Encoder.encode(null));
        assertNull(Decoder.decode(null));
        assertNull(Decoder.decode(""));
        assertEquals("plain", Decoder.decode2string(null, null, "plain"));
    }

    /**
     * Rejects invalid gzip, truncation, unknown transfer encodings, and malformed decoded XML.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void corruptCompressedDataAndUnknownEncodingsFailWithTheirCause() throws Exception {
        String badGzip = Base64.getEncoder().encodeToString("not gzip".getBytes(StandardCharsets.UTF_8));
        assertThrows(IOException.class, () -> Decoder.decode(badGzip));
        String good = encoded("base64", ContentType.XGZIP, TEXT.getBytes(StandardCharsets.UTF_8));
        byte[] compressed = Base64.getDecoder().decode(good.replaceAll("\\s", ""));
        String truncated = Base64.getEncoder().encodeToString(java.util.Arrays.copyOf(compressed, 8));
        assertThrows(IOException.class, () -> Decoder.decode(truncated));
        IOException error = assertThrows(IOException.class, () -> Decoder.decode2string("unsupported", ContentType.TEXT, "x"));
        assertInstanceOf(jakarta.mail.MessagingException.class, error.getCause());
        assertThrows(IOException.class, () -> Decoder.decode2dom("7bit", ContentType.TEXT, "<bad>"));
    }

    /**
     * Checks gzip materialization, pass-through identity, and transfer-decoding stream overloads.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void contentTypeOverloadsHandleUtf8GzipAndPlainData() throws Exception {
        byte[] text = TEXT.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (OutputStream stream = ContentType.encode(ContentType.XGZIP, compressed)) { stream.write(text); }
        assertArrayEquals(text, ContentType.decode2ByteArray(ContentType.XGZIP, new ByteArrayInputStream(compressed.toByteArray())));
        assertEquals(TEXT, ContentType.decode2String(ContentType.XGZIP, new ByteArrayInputStream(compressed.toByteArray())));
        assertEquals(TEXT, ContentType.decode2String(ContentType.TEXT, new ByteArrayInputStream(text)));
        ByteArrayInputStream plain = new ByteArrayInputStream(text);
        assertSame(plain, ContentType.decode(ContentType.TEXT, plain));
        assertArrayEquals(text, ContentType.decode2ByteArray(ContentType.TEXT, plain));
        String transfer = Base64.getEncoder().encodeToString(text);
        assertArrayEquals(text, ContentTransferEncoding.decode2ByteArray("base64", new ByteArrayInputStream(transfer.getBytes(StandardCharsets.US_ASCII))));
        try (var stream = ContentTransferEncoding.decode("base64", new ByteArrayInputStream(transfer.getBytes(StandardCharsets.US_ASCII)))) {
            assertArrayEquals(text, stream.readAllBytes());
        }
    }
}
