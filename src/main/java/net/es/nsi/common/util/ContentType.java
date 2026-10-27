package net.es.nsi.common.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CloseShieldInputStream;

/**
 * Applies gzip content compression or passes bytes through for other content types.
 *
 * <p>Only {@code application/x-gzip} is treated specially, ignoring case. Materializing
 * methods consume but do not close borrowed inputs. Callers own wrappers returned by
 * stream methods, including responsibility for finishing gzip output.
 *
 * @author hacksaw
 */
public class ContentType {
    /**
     * Content type selecting gzip compression.
     */
    public final static String XGZIP = "application/x-gzip";
    /**
     * XML content type; bytes pass through unchanged.
     */
    public final static String XML = "application/xml";
    /**
     * Plain-text content type; bytes pass through unchanged.
     */
    public final static String TEXT = "text/plain";

    /**
     * Returns a gzip decoder for the gzip content type, otherwise the original stream.
     * Closing a decoder closes its underlying input.
     *
     * @param contentType content type, or null for pass-through
     * @param is non-null source stream
     * @return an open decoding stream
     * @throws IOException if a gzip stream cannot be initialized
     */
    public static InputStream decode(String contentType, InputStream is) throws IOException {
        if (XGZIP.equalsIgnoreCase(contentType)) {
            return new GZIPInputStream(is);
        }
        else {
            return is;
        }
    }

    /**
     * Reads and optionally decompresses all bytes, leaving the supplied input open.
     *
     * @param contentType content type, or null for pass-through
     * @param is non-null source stream
     * @return the decoded bytes
     * @throws IOException if the input is unreadable or invalid gzip data
     */
    public static byte[] decode2ByteArray(String contentType, InputStream is) throws IOException {
        if (XGZIP.equalsIgnoreCase(contentType)) {
            // Release the gzip wrapper without taking ownership of the borrowed input.
            try (GZIPInputStream decoded = new GZIPInputStream(CloseShieldInputStream.wrap(is))) {
                return IOUtils.toByteArray(decoded);
            }
        }
        else {
            return IOUtils.toByteArray(is);
        }
    }

    /**
     * Decodes content as UTF-8 text, leaving the supplied input open.
     *
     * @param contentType content type, or null for pass-through
     * @param is non-null source stream
     * @return the decoded text
     * @throws IOException if reading or decompression fails
     */
    public static String decode2String(String contentType, InputStream is) throws IOException {
        return new String(decode2ByteArray(contentType, is), StandardCharsets.UTF_8);
    }

    /**
     * Returns a gzip encoder for the gzip content type, otherwise the original output.
     * Close the returned wrapper to complete the gzip trailer and close the destination.
     *
     * @param contentType content type, or null for pass-through
     * @param os non-null destination stream
     * @return an encoding stream
     * @throws IOException if gzip output cannot be initialized
     */
    public static OutputStream encode(String contentType, OutputStream os) throws IOException {
        if (XGZIP.equalsIgnoreCase(contentType)) {
            return new GZIPOutputStream(os);
        }
        else {
            return os;
        }
    }
}
