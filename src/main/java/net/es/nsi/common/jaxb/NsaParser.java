package net.es.nsi.common.jaxb;

import java.io.IOException;
import java.io.InputStream;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import net.es.nsi.common.jaxb.nsa.NsaType;
import net.es.nsi.common.jaxb.nsa.ObjectFactory;

/**
 * Singleton parser for generated NSA discovery bindings.
 *
 * <p>The cached JAXB context is initialized on first use; each read or write gets
 * independent JAXB operation state. Callers coordinate access to shared models and files.
 *
 * @author hacksaw
 */
public class NsaParser extends JaxbParser {

  /**
   * Generated discovery binding package used to initialize the shared JAXB context.
   */
  private static final String PACKAGES = "net.es.nsi.common.jaxb.nsa";
  /**
   * Schema object factory that supplies the discovery root QName.
   */
  private static final ObjectFactory FACTORY = new ObjectFactory();

  /**
   * Initializes the generated NSA discovery binding context.
   */
  private NsaParser() {
    super(PACKAGES);
  }

  /**
   * Defers singleton creation until first access; class initialization publishes it safely.
   */
  private static class ParserHolder {

    /**
     * The lazily initialized parser instance.
     */
    public static final NsaParser INSTANCE = new NsaParser();
  }

  /**
   * Returns the lazily initialized NSA discovery parser.
   *
   * @return the shared parser instance
   */
  public static NsaParser getInstance() {
    return ParserHolder.INSTANCE;
  }

  /**
   * Reads a NSA discovery document from a file, closing owned input streams.
   *
   * @param filename source XML path
   * @return the parsed document model
   * @throws JAXBException if XML or its root binding is invalid
   * @throws IOException if the source cannot be read
   */
  public NsaType readDocument(String filename) throws JAXBException, IOException {
    return this.parseFile(NsaType.class, filename);
  }

  /**
   * Reads a NSA discovery document from a borrowed byte stream.
   *
   * @param is non-null XML stream, left open for the caller
   * @return the parsed document model
   * @throws JAXBException if XML or its root binding is invalid
   * @throws IOException if the source cannot be read
   */
  public NsaType readDocument(InputStream is) throws JAXBException, IOException {
    return this.xml2Jaxb(NsaType.class, is);
  }

  /**
   * Writes a NSA discovery model using its schema-defined root element.
   *
   * @param file destination XML path, replaced if it exists
   * @param nsa non-null document model
   * @throws JAXBException if marshalling fails
   * @throws IOException if the destination cannot be created or written
   */
  public void writeDocument(String file, NsaType nsa) throws JAXBException, IOException {
    // Supply the schema root QName, since generated value types have no root annotation.
    JAXBElement<NsaType> element = FACTORY.createNsa(nsa);
    this.writeFile(element, file);
  }
}
