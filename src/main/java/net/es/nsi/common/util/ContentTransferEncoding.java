package net.es.nsi.common.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeUtility;
import org.apache.commons.io.IOUtils;

/**
 * Applies MIME content-transfer encodings through Jakarta Mail.
 *
 * <p>String inputs become UTF-8 bytes before decoding. Stream-returning methods transfer
 * ownership of their wrapper to the caller; closing a wrapper can close its underlying
 * stream. Byte-array methods consume but do not close caller-supplied streams.
 *
 * @author hacksaw
 */
public class ContentTransferEncoding {
    /**
     * MIME Base64 transfer encoding.
     */
    public final static String BASE64 = "base64";
    /**
     * MIME quoted-printable transfer encoding.
     */
    public final static String QUOTEDPRINTABLE = "quoted-printable";
    /**
     * MIME 8-bit identity transfer encoding.
     */
    public final static String _8BIT = "8bit";
    /**
     * MIME 7-bit identity transfer encoding.
     */
    public final static String _7BIT = "7bit";
    /**
     * MIME binary identity transfer encoding.
     */
    public final static String BINARY = "binary";
    /**
     * Legacy uuencode transfer encoding.
     */
    public final static String UUENCODE = "uuencode";

    /**
     * Creates a decoding stream. The caller must close the returned stream.
     *
     * @param contentTransferEncoding MIME transfer-encoding name
     * @param source non-null encoded text interpreted as UTF-8
     * @return an open stream yielding decoded bytes
     * @throws UnsupportedEncodingException if the requested transfer encoding is unsupported
     * @throws MessagingException if the MIME decoder cannot be created
     */
    public static InputStream decode(String contentTransferEncoding, String source) throws UnsupportedEncodingException, MessagingException {
        return MimeUtility.decode(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)), contentTransferEncoding);
    }

    /**
     * Creates a decoding stream. The caller must close the returned stream.
     *
     * @param contentTransferEncoding MIME transfer-encoding name
     * @param source non-null encoded stream
     * @return an open stream yielding decoded bytes
     * @throws UnsupportedEncodingException if the requested transfer encoding is unsupported
     * @throws MessagingException if the MIME decoder cannot be created
     */
    public static InputStream decode(String contentTransferEncoding, InputStream source) throws UnsupportedEncodingException, MessagingException {
        return MimeUtility.decode(source, contentTransferEncoding);
    }

    /**
     * Decodes all input bytes.
     *
     * @param contentTransferEncoding MIME transfer-encoding name
     * @param source non-null encoded text interpreted as UTF-8
     * @return the decoded bytes
     * @throws UnsupportedEncodingException if the requested transfer encoding is unsupported
     * @throws MessagingException if the MIME decoder cannot be created
     * @throws IOException if reading the encoded input fails
     */
    public static byte[] decode2ByteArray(String contentTransferEncoding, String source) throws UnsupportedEncodingException, MessagingException, IOException {
        return IOUtils.toByteArray(MimeUtility.decode(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)), contentTransferEncoding));
    }

    /**
     * Decodes all input bytes without closing the supplied stream.
     *
     * @param contentTransferEncoding MIME transfer-encoding name
     * @param source non-null encoded stream
     * @return the decoded bytes
     * @throws UnsupportedEncodingException if the requested transfer encoding is unsupported
     * @throws MessagingException if the MIME decoder cannot be created
     * @throws IOException if reading the encoded input fails
     */
    public static byte[] decode2ByteArray(String contentTransferEncoding, InputStream source) throws UnsupportedEncodingException, MessagingException, IOException {
        return IOUtils.toByteArray(MimeUtility.decode(source, contentTransferEncoding));
    }

    /**
     * Creates a MIME encoding stream. Close the returned stream to flush final encoded
     * bytes; this can also close the destination.
     *
     * @param contentTransferEncoding MIME transfer-encoding name
     * @param os non-null destination for encoded bytes
     * @return an encoding wrapper, or the destination for an identity encoding
     * @throws MessagingException if the encoding is unsupported or cannot be created
     */
    public static OutputStream encode(String contentTransferEncoding, OutputStream os) throws MessagingException {
        return MimeUtility.encode(os, contentTransferEncoding);
    }
}
