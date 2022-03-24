package net.es.nsi.common.jaxb;

import java.io.IOException;
import java.io.InputStream;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import net.es.nsi.common.jaxb.nml.NmlTopologyType;
import net.es.nsi.common.jaxb.nml.ObjectFactory;

/**
 * A singleton to load the very expensive NMWG JAXBContext once.
 *
 * @author hacksaw
 */
public class NmlParser extends JaxbParser {

  private static final String PACKAGES = "net.es.nsi.common.jaxb.nml";
  private static final ObjectFactory factory = new ObjectFactory();

  private NmlParser() {
    super(PACKAGES);
  }

  /**
   * An internal static class that invokes our private constructor on object creation.
   */
  private static class ParserHolder {

    public static final NmlParser INSTANCE = new NmlParser();
  }

  /**
   * Returns an instance of this singleton class.
   *
   * @return An object of the NmwgParser.
   */
  public static NmlParser getInstance() {
    return ParserHolder.INSTANCE;
  }

  public NmlTopologyType readDocument(String filename) throws JAXBException, IOException {
    return this.parseFile(NmlTopologyType.class, filename);
  }

  public NmlTopologyType readDocument(InputStream is) throws JAXBException, IOException {
    return this.xml2Jaxb(NmlTopologyType.class, is);
  }

  public void writeDocument(String file, NmlTopologyType nml) throws JAXBException, IOException {
    // Parse the specified file.
    JAXBElement<NmlTopologyType> element = factory.createTopology(nml);
    this.writeFile(element, file);
  }
}
