package net.es.nsi.common.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;
import net.es.nsi.common.jaxb.DomParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;

/**
 * Serializes DOM XML as UTF-8, gzip-compresses it, and returns Base64 text.
 *
 * <p>The output uses the basic Base64 alphabet without MIME line wrapping and can be
 * read by {@link Decoder#decode(String)}.
 *
 * @author hacksaw
 */
public class Encoder {
  /**
   * Logger for XML serialization and compression failures.
   */
  private final static Logger LOG = LogManager.getLogger(Encoder.class);

  /**
   * Encodes a document without modifying its DOM.
   *
   * @param doc document to encode, or null
   * @return Base64-encoded gzip XML, or null for a null document
   * @throws IOException if XML serialization or compression fails
   */
  public static String encode(Document doc) throws IOException {
    if (doc == null) {
      return null;
    }

    String toEncode;
    try {
      toEncode = DomParser.doc2Xml(doc);
    } catch (Exception ex) {
      LOG.error("Encoder: failed to serialize XML document", ex);
      throw new IOException(ex);
    }
    byte[] compressed = compress(toEncode.getBytes(Charset.forName("UTF-8")));
    String encoded = Base64.getEncoder().encodeToString(compressed);
    return encoded;
  }

  /**
   * Compresses an in-memory byte sequence into a complete gzip member.
   *
   * @param source bytes to compress
   * @return compressed bytes including the gzip trailer
   * @throws IOException if compression fails
   */
  private static byte[] compress(byte[] source) throws IOException {
    try (ByteArrayOutputStream os = new ByteArrayOutputStream(source.length)) {
      // gzip closes its compressor before bytes are copied, so the trailer is included.
      return gzip(os, source).toByteArray();
    } catch (IOException io) {
      LOG.error("Failed to compress source", io);
      throw io;
    }
  }

  /**
   * Writes a complete gzip member and closes the compressor before returning.
   * The byte-array destination remains readable after closure.
   *
   * @param os in-memory destination
   * @param source uncompressed bytes
   * @return the destination containing finished gzip data
   * @throws IOException if writing or finishing gzip data fails
   */
  private static ByteArrayOutputStream gzip(ByteArrayOutputStream os, byte[] source) throws IOException {
    try (GZIPOutputStream gos = new GZIPOutputStream(os)) {
      gos.write(source);
      return os;
    } catch (IOException io) {
      LOG.error("Failed to gzip source stream", io);
      throw io;
    }
  }
}
