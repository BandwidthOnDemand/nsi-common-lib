package net.es.nsi.common.util;

import jakarta.mail.MessagingException;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/**
 * Tests MIME transfer decoding and encoding with non-ASCII UTF-8 payloads.
 */
class ContentTransferEncodingTest {

    /**
     * Checks quoted-printable byte escapes against the expected UTF-8 bytes.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void decodesQuotedPrintableUtf8() throws Exception {
        byte[] decoded = ContentTransferEncoding.decode2ByteArray(
                ContentTransferEncoding.QUOTEDPRINTABLE, "caf=C3=A9=20NSI");

        assertArrayEquals("café NSI".getBytes(StandardCharsets.UTF_8), decoded);
    }

    /**
     * Checks that closing the Base64 encoder flushes all bytes before decoding.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void base64RoundTrip() throws Exception {
        byte[] original = "NSI topology \u2603\n".getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        try (OutputStream output = ContentTransferEncoding.encode(
                ContentTransferEncoding.BASE64, encoded)) {
            output.write(original);
        }

        byte[] decoded = ContentTransferEncoding.decode2ByteArray(
                ContentTransferEncoding.BASE64, encoded.toString(StandardCharsets.US_ASCII));

        assertArrayEquals(original, decoded);
    }

    /**
     * Requires an explicit messaging failure for an unsupported transfer-encoding name.
     */
    @Test
    void rejectsUnknownTransferEncoding() {
        assertThrows(MessagingException.class,
                () -> ContentTransferEncoding.decode("unknown-encoding", "NSI"));
    }
}
