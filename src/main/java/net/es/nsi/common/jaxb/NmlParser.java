package net.es.nsi.common.jaxb;

import java.io.IOException;
import java.io.InputStream;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import net.es.nsi.common.jaxb.nml.NmlTopologyType;
import net.es.nsi.common.jaxb.nml.ObjectFactory;

/**
 * Singleton parser for generated NML topology bindings.
 *
 * <p>The cached JAXB context is initialized on first use; each read or write gets
 * independent JAXB operation state. Callers coordinate access to shared models and files.
 *
 * @author hacksaw
 */
public class NmlParser extends JaxbParser {

  /**
   * Generated topology binding package used to initialize the shared JAXB context.
   */
  private static final String PACKAGES = "net.es.nsi.common.jaxb.nml";
  /**
   * Schema object factory that supplies the topology root QName.
   */
  private static final ObjectFactory factory = new ObjectFactory();

  /**
   * Initializes the generated NML topology binding context.
   */
  private NmlParser() {
    super(PACKAGES);
  }

  /**
   * Defers singleton creation until first access; class initialization publishes it safely.
   */
  private static class ParserHolder {

    /**
     * The lazily initialized parser instance.
     */
    public static final NmlParser INSTANCE = new NmlParser();
  }

  /**
   * Returns the lazily initialized NML topology parser.
   *
   * @return the shared parser instance
   */
  public static NmlParser getInstance() {
    return ParserHolder.INSTANCE;
  }

  /**
   * Reads a NML topology document from a file, closing owned input streams.
   *
   * @param filename source XML path
   * @return the parsed document model
   * @throws JAXBException if XML or its root binding is invalid
   * @throws IOException if the source cannot be read
   */
  public NmlTopologyType readDocument(String filename) throws JAXBException, IOException {
    return this.parseFile(NmlTopologyType.class, filename);
  }

  /**
   * Reads a NML topology document from a borrowed byte stream.
   *
   * @param is non-null XML stream, left open for the caller
   * @return the parsed document model
   * @throws JAXBException if XML or its root binding is invalid
   * @throws IOException if the source cannot be read
   */
  public NmlTopologyType readDocument(InputStream is) throws JAXBException, IOException {
    return this.xml2Jaxb(NmlTopologyType.class, is);
  }

  /**
   * Writes a NML topology model using its schema-defined root element.
   *
   * @param file destination XML path, replaced if it exists
   * @param nml non-null document model
   * @throws JAXBException if marshalling fails
   * @throws IOException if the destination cannot be created or written
   */
  public void writeDocument(String file, NmlTopologyType nml) throws JAXBException, IOException {
    // Supply the schema root QName, since generated value types have no root annotation.
    JAXBElement<NmlTopologyType> element = factory.createTopology(nml);
    this.writeFile(element, file);
  }
}
