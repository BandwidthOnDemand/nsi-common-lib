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
 *
 * @author hacksaw
 */
public class Encoder {
  private final static Logger LOG = LogManager.getLogger(Encoder.class);

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

  private static byte[] compress(byte[] source) throws IOException {
    try (ByteArrayOutputStream os = new ByteArrayOutputStream(source.length)) {
      return gzip(os, source).toByteArray();
    } catch (IOException io) {
      LOG.error("Failed to compress source", io);
      throw io;
    }
  }

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
